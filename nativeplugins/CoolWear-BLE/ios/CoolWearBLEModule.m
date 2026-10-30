#import "CoolWearBLEModule.h"
#import <BluetoothLibrary/BluetoothLibrary.h>
#import <CoreBluetooth/CoreBluetooth.h>

@interface CoolWearBLEModule () <CBCentralManagerDelegate>
@property (nonatomic, strong) CBCentralManager *permCentral;
@property (nonatomic, strong) CBCentralManager *stateCentral;
@property (nonatomic, copy) UniModuleKeepAliveCallback permCallback;
@end

static NSString * const kEventChannel = @"CoolWearBLE";

@interface CoolWearBLEModule ()

@property (nonatomic, assign) BOOL initialized;
@property (nonatomic, assign) BOOL observersRegistered;
@property (nonatomic, strong) NSMutableDictionary<NSString *, CBPeripheral *> *peripheralMap;
@property (nonatomic, strong) NSMutableSet<NSString *> *emittedDevices;
@property (nonatomic, strong) NSMutableArray<NSDictionary *> *healthCache;
@property (nonatomic, strong) NSData *otaFileData;
@property (nonatomic, strong) NSDictionary *cachedDeviceInfo;
@property (nonatomic, strong) NSDictionary *cachedOtaStatusInfo;

@end

static const NSUInteger kHealthCacheLimit = 300;

@implementation CoolWearBLEModule

+ (void)load {
}

- (instancetype)init {
    self = [super init];
    if (self) {
        _peripheralMap = [NSMutableDictionary dictionary];
        _emittedDevices = [NSMutableSet set];
        _healthCache = [NSMutableArray array];
    }
    return self;
}

- (void)dealloc {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

#pragma mark - Helpers

- (NSDictionary *)okResult:(id)data {
    if (data) {
        return @{@"code": @0, @"message": @"ok", @"data": data};
    }
    return @{@"code": @0, @"message": @"ok"};
}

- (NSDictionary *)errResult:(NSString *)message {
    return @{@"code": @(-1), @"message": message ?: @"error"};
}

- (NSDictionary *)errResultCode:(NSInteger)code message:(NSString *)message {
    return @{@"code": @(code), @"message": message ?: @"error"};
}

- (void)cacheHealthEvent:(NSString *)type data:(id)data {
    if (!type || !data) return;
    @synchronized (self.healthCache) {
        [self.healthCache addObject:@{@"type": type, @"data": data, @"at": @((long long)([[NSDate date] timeIntervalSince1970] * 1000))}];
        if (self.healthCache.count > kHealthCacheLimit) {
            [self.healthCache removeObjectsInRange:NSMakeRange(0, self.healthCache.count - kHealthCacheLimit)];
        }
    }
}

- (void)fireEvent:(NSString *)event data:(id)data {
    if (!self.uniInstance) {
        return;
    }
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.uniInstance fireGlobalEvent:kEventChannel params:@{
            @"event": event ?: @"",
            @"data": data ?: @{}
        }];
    });
}

- (void)registerObserversIfNeeded {
    if (self.observersRegistered) {
        return;
    }
    NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
    [center addObserver:self selector:@selector(onScanResult:) name:ScanPeripheralsNoticeKey object:nil];
    [center addObserver:self selector:@selector(onStatusChange:) name:ProductStatusChangeNoticeKey object:nil];
    [center addObserver:self selector:@selector(onReceiveData:) name:CEProductK6ReceiveDataNoticeKey object:nil];
    self.observersRegistered = YES;
}

- (NSNumber *)mapProductStatus:(ProductStatus)status {
    switch (status) {
        case ProductStatus_connecting:
            return @2;
        case ProductStatus_connected:
        case ProductStatus_completed:
            return @1;
        default:
            return @0;
    }
}

#pragma mark - Notifications

- (void)onScanResult:(NSNotification *)notification {
    NSArray *list = notification.object;
    if (![list isKindOfClass:[NSArray class]]) {
        return;
    }
    for (SearchPeripheral *item in list) {
        if (![item isKindOfClass:[SearchPeripheral class]] || !item.peripheral) {
            continue;
        }
        NSString *uuid = item.peripheral.identifier.UUIDString;
        if (!uuid || [self.emittedDevices containsObject:uuid]) {
            continue;
        }
        [self.emittedDevices addObject:uuid];
        [self.peripheralMap setObject:item.peripheral forKey:uuid];
        NSMutableDictionary *payload = [NSMutableDictionary dictionary];
        payload[@"name"] = [item name] ?: @"";
        payload[@"identifier"] = uuid;
        payload[@"mac"] = [item macAddress] ?: @"";
        if (item.rssi) {
            payload[@"rssi"] = item.rssi;
        }
        payload[@"deviceId"] = [item deviceID] ?: @"";
        [self fireEvent:@"onDeviceFound" data:payload];
    }
}

- (void)onStatusChange:(NSNotification *)notification {
    NSNumber *statusNum = notification.object;
    if (![statusNum isKindOfClass:[NSNumber class]]) {
        return;
    }
    ProductStatus status = (ProductStatus)[statusNum integerValue];
    if (status == ProductStatus_completed) {
        [[CEProductK6 shareInstance] saveConnectedUUid];
        [CE_SensorCmd open];
    }
    [self fireEvent:@"onConnectStateChange" data:@{
        @"status": [self mapProductStatus:status],
        @"productStatus": statusNum
    }];
}

- (id)pickFirst:(NSDictionary *)dict keys:(NSArray<NSString *> *)keys {
    for (NSString *k in keys) {
        id v = dict[k];
        if (v != nil && v != [NSNull null]) return v;
    }
    return nil;
}

- (NSArray *)normalizedValueArray:(id)data valueKeys:(NSArray<NSString *> *)vKeys extra:(NSDictionary *(^)(NSDictionary *src, NSMutableDictionary *out))extraBlock {
    // iOS SDK 包一层 {curItemCount, sportInfos/heartInfos/data:[...], remainItemCount}，先拆平
    if ([data isKindOfClass:[NSDictionary class]]) {
        id inner = ((NSDictionary *)data)[@"sportInfos"] ?: ((NSDictionary *)data)[@"heartInfos"] ?: ((NSDictionary *)data)[@"data"] ?: ((NSDictionary *)data)[@"list"] ?: ((NSDictionary *)data)[@"items"];
        if ([inner isKindOfClass:[NSArray class]]) data = inner;
    }
    NSArray *list = nil;
    if ([data isKindOfClass:[NSArray class]]) list = data;
    else if (data != nil) list = @[data];
    else list = @[];
    NSMutableArray *flat = [NSMutableArray array];
    for (id w in list) {
        if ([w isKindOfClass:[NSDictionary class]]) {
            id inner = ((NSDictionary *)w)[@"sportInfos"] ?: ((NSDictionary *)w)[@"heartInfos"] ?: ((NSDictionary *)w)[@"data"];
            if ([inner isKindOfClass:[NSArray class]]) { [flat addObjectsFromArray:inner]; continue; }
        }
        [flat addObject:w ?: @{}];
    }
    list = flat;
    NSMutableArray *out = [NSMutableArray arrayWithCapacity:list.count];
    for (id item in list) {
        if ([item isKindOfClass:[NSDictionary class]]) {
            NSDictionary *d = item;
            NSMutableDictionary *o = [NSMutableDictionary dictionary];
            id v = [self pickFirst:d keys:vKeys];
            if (v == nil) {
                for (id k in d) { id vv = d[k]; if ([vv isKindOfClass:[NSNumber class]]) { v = vv; break; } }
            }
            if (v != nil) o[@"value"] = v;
            id hn = [self pickFirst:d keys:@[@"heartNum", @"heartNums", @"heart", @"hr"]];
            if (hn != nil) { o[@"heart"] = hn; o[@"hr"] = hn; if (o[@"value"] == nil) o[@"value"] = hn; }
            id ox = [self pickFirst:d keys:@[@"oxygen", @"o2", @"spo2"]];
            if (ox != nil) { o[@"o2"] = ox; o[@"oxygen"] = ox; if (o[@"value"] == nil) o[@"value"] = ox; }
            id t = [self pickFirst:d keys:@[@"time", @"timestamp", @"measureTime", @"date", @"datetime", @"startSecs", @"startTime"]];
            if (t != nil) o[@"time"] = t;
            if (extraBlock) { NSDictionary *ex = extraBlock(d, o); if (ex) [o addEntriesFromDictionary:ex]; }
            o[@"raw"] = d;
            [out addObject:o];
        } else if ([item isKindOfClass:[NSNumber class]]) {
            [out addObject:@{@"value": item, @"raw": @{@"value": item}}];
        } else {
            [out addObject:@{@"value": item ?: @"", @"raw": [NSString stringWithFormat:@"%@", item]}];
        }
    }
    return out;
}

- (void)onReceiveData:(NSNotification *)notification {
    NSDictionary *userInfo = notification.userInfo;
    if (![userInfo isKindOfClass:[NSDictionary class]]) {
        return;
    }
    NSNumber *dataType = userInfo[@"DataType"];
    id data = userInfo[@"Data"];
    if (!dataType) {
        return;
    }
    K6_DataFuncType type = (K6_DataFuncType)[dataType integerValue];
    switch (type) {
        case DATA_TYPE_BATTERY_INFO: {
            NSLog(@"[CoolWearBLE] battery raw class=%@ value=%@", NSStringFromClass([data class]), data);
            NSMutableDictionary *payload = [NSMutableDictionary dictionary];
            if ([data isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = (NSDictionary *)data;
                NSArray *levelKeys = @[@"level", @"battery", @"Battery", @"power", @"powerValue", @"battery_capacity", @"batteryCapacity", @"electricity", @"capacity", @"value", @"percent", @"percentage", @"battLevel", @"charge"];
                id level = nil;
                for (NSString *k in levelKeys) {
                    id v = dict[k];
                    if (v != nil && v != [NSNull null]) { level = v; break; }
                }
                // 兜底：字典里第一个 NSNumber 值
                if (level == nil) {
                    for (id k in dict) {
                        id v = dict[k];
                        if ([v isKindOfClass:[NSNumber class]]) { level = v; break; }
                    }
                }
                NSArray *chargeKeys = @[@"charging", @"chargerStatus", @"isChargerStatus", @"isCharging", @"deviceChargerStatus", @"chargeStatus", @"isCharge", @"charger", @"chargingStatus"];
                id charging = nil;
                for (NSString *k in chargeKeys) {
                    id v = dict[k];
                    if (v != nil && v != [NSNull null]) { charging = v; break; }
                }
                if (level != nil) payload[@"level"] = @([level intValue]);
                BOOL isCharging = NO;
                if ([charging respondsToSelector:@selector(boolValue)]) isCharging = [charging boolValue];
                payload[@"charging"] = @(isCharging);
                payload[@"chargerStatus"] = @(isCharging);
                payload[@"isChargerStatus"] = @(isCharging);
                payload[@"raw"] = dict;
            } else if ([data isKindOfClass:[NSNumber class]]) {
                // iOS SDK 有时直接透传 NSNumber 电量
                payload[@"level"] = @([(NSNumber *)data intValue]);
                payload[@"charging"] = @NO;
                payload[@"chargerStatus"] = @NO;
                payload[@"isChargerStatus"] = @NO;
                payload[@"raw"] = @{@"value": data};
            } else {
                payload[@"raw"] = data ? [NSString stringWithFormat:@"%@", data] : @"";
            }
            [self fireEvent:@"onBattery" data:payload];
            break;
        }
        case DATA_TYPE_REAL_HEART:
        case DATA_TYPE_EXERCISE_HEART:
        case DATA_TYPE_REAL_HR: {
            NSArray *arr = [self normalizedValueArray:data valueKeys:@[@"value", @"heart", @"heartNums", @"heartNum", @"hr", @"bpm", @"heartRate"] extra:nil];
            [self fireEvent:@"onHeartRate" data:arr];
            break;
        }
        case DATA_TYPE_HISTORY_HEART: {
            NSArray *arr = [self normalizedValueArray:data valueKeys:@[@"value", @"heart", @"heartNums", @"heartNum", @"hr", @"bpm", @"heartRate"] extra:nil];
            [self cacheHealthEvent:@"dailyHeart" data:arr];
            [self fireEvent:@"onHeartRateDaily" data:arr];
            break;
        }
        case DATA_TYPE_REAL_HRV:
        case DATA_TYPE_HISTORY_HRV: {
            NSArray *arr = [self normalizedValueArray:data valueKeys:@[@"value", @"hrv", @"hrvNums", @"hrvValue", @"rmssd", @"sdnn", @"heartNum", @"heart"] extra:^NSDictionary *(NSDictionary *src, NSMutableDictionary *out) {
                id s = [self pickFirst:src keys:@[@"stress", @"pressure", @"stressValue", @"press"]];
                if (s != nil) return @{@"stress": s, @"pressure": s};
                return nil;
            }];
            [self cacheHealthEvent:@"hrv" data:arr];
            [self fireEvent:@"onHRV" data:arr];
            break;
        }
        case DATA_TYPE_HISTORY_TEMP: {
            NSArray *arr = [self normalizedValueArray:data valueKeys:@[@"value", @"temp", @"tempValue", @"temperature"] extra:nil];
            [self fireEvent:@"onTemperature" data:arr];
            break;
        }
        case DATA_TYPE_SLEEP: {
            NSDictionary *payload = nil;
            if ([data isKindOfClass:[NSDictionary class]]) {
                NSMutableDictionary *p = [NSMutableDictionary dictionaryWithDictionary:data];
                // SLEEP_NONE=0, SLEEP_START=1, SLEEP_DEEP=2, SLEEP_LIGHT=3, SLEEP_WAKEUP=4
                if (!p[@"typeMap"]) p[@"typeMap"] = @{@"start": @1, @"deep": @2, @"light": @3, @"movement": @3, @"wakeup": @4};
                if (!p[@"sessions"] && !p[@"sleeps"]) p[@"sessions"] = @[data];
                p[@"raw"] = data;
                payload = p;
            } else {
                payload = @{@"sessions": data ?: @[], @"raw": data ?: @[]};
            }
            [self cacheHealthEvent:@"sleep" data:payload];
            [self fireEvent:@"onSleep" data:payload];
            break;
        }
        case DATA_TYPE_REAL_O2:
        case DATA_TYPE_HISTORY_O2: {
            NSArray *arr = [self normalizedValueArray:data valueKeys:@[@"value", @"o2", @"oxygen", @"spo2", @"bloodOxygen", @"oxygenValue", @"o2Value", @"O2", @"O2Value"] extra:^NSDictionary *(NSDictionary *src, NSMutableDictionary *out) {
                id v = out[@"value"];
                if (v != nil) return @{@"o2": v};
                return nil;
            }];
            [self cacheHealthEvent:@"spo2" data:arr];
            [self fireEvent:@"onBloodOxygen" data:arr];
            break;
        }
        case DATA_TYPE_REAL_SPORT:
        case DATA_TYPE_HISTORY_SPORT: {
            NSArray *list = [data isKindOfClass:[NSArray class]] ? data : (data ? @[data] : @[]);
            // iOS SDK 包一层 {sportInfos:[...]}，先拆平成单条
            NSMutableArray *flat = [NSMutableArray array];
            for (id w in list) {
                if ([w isKindOfClass:[NSDictionary class]] && [(NSDictionary *)w objectForKey:@"sportInfos"]) {
                    id infos = [(NSDictionary *)w objectForKey:@"sportInfos"];
                    if ([infos isKindOfClass:[NSArray class]]) [flat addObjectsFromArray:infos];
                    else if (infos) [flat addObject:infos];
                } else { [flat addObject:w ?: @{}]; }
            }
            NSMutableArray *arr = [NSMutableArray array];
            for (id item in flat) {
                if ([item isKindOfClass:[NSDictionary class]]) {
                    NSDictionary *d = item;
                    NSMutableDictionary *o = [NSMutableDictionary dictionary];
                    id steps = [self pickFirst:d keys:@[@"walkSteps", @"steps", @"step", @"walk_steps"]];
                    if (steps != nil) { o[@"walkSteps"] = steps; o[@"steps"] = steps; }
                    id dist = [self pickFirst:d keys:@[@"distance", @"walkDistance", @"meters"]];
                    if (dist != nil) o[@"distance"] = dist;
                    id cal = [self pickFirst:d keys:@[@"calories", @"calorie", @"kcal", @"walkCalories"]];
                    if (cal != nil) o[@"calories"] = cal;
                    id t = [self pickFirst:d keys:@[@"time", @"timestamp", @"measureTime", @"date", @"startSecs", @"startTime"]];
                    if (t != nil) {
                        // startSecs 是秒，转毫秒与安卓对齐
                        if ([d objectForKey:@"startSecs"] == t) o[@"time"] = @([t longLongValue] * 1000);
                        else o[@"time"] = t;
                    }
                    o[@"raw"] = d;
                    [arr addObject:o];
                } else { [arr addObject:@{@"raw": [NSString stringWithFormat:@"%@", item]}]; }
            }
            [self fireEvent:@"onSport" data:arr];
            break;
        }
        case DATA_TYPE_MIX_SPORT: {
            NSArray *list = [data isKindOfClass:[NSArray class]] ? data : (data ? @[data] : @[]);
            NSMutableArray *arr = [NSMutableArray array];
            for (id item in list) {
                if ([item isKindOfClass:[NSDictionary class]]) [arr addObject:item];
                else [arr addObject:@{@"raw": [NSString stringWithFormat:@"%@", item]}];
            }
            [self fireEvent:@"onMixSport" data:arr];
            break;
        }
        case DATA_TYPE_DEVINFO: {
            if ([data isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = (NSDictionary *)data;
                self.cachedDeviceInfo = dict;
                NSMutableDictionary *payload = [NSMutableDictionary dictionaryWithDictionary:dict];
                id ver = [self pickFirst:dict keys:@[@"softwareVer", @"firmware", @"SoftwareVer", @"softwareVersion", @"version", @"softVersion", @"fwVersion"]];
                if (ver != nil) {
                    NSString *verStr = [NSString stringWithFormat:@"%@", ver];
                    payload[@"softwareVer"] = verStr;
                    payload[@"firmware"] = verStr;
                }
                id hw = [self pickFirst:dict keys:@[@"hardwareVer", @"HardwareVer", @"hardwareVersion", @"hardVersion", @"hwVersion"]];
                if (hw != nil) payload[@"hardwareVer"] = [NSString stringWithFormat:@"%@", hw];
                id cid = [self pickFirst:dict keys:@[@"customerId", @"customer_id", @"customerID", @"cid"]];
                if (cid != nil) { payload[@"customerId"] = cid; }
                id did = [self pickFirst:dict keys:@[@"deviceId", @"deviceID", @"devId"]];
                if (did != nil) { payload[@"deviceId"] = did; payload[@"deviceID"] = did; }
                if (!payload[@"raw"]) payload[@"raw"] = dict;
                [self fireEvent:@"onDeviceInfo" data:payload];
            } else {
                [self fireEvent:@"onDeviceInfo" data:@{@"raw": data ?: @""}];
            }
            break;
        }
        case DATA_TYPE_OTA_STATUS: {
            if ([data isKindOfClass:[NSDictionary class]]) {
                self.cachedOtaStatusInfo = (NSDictionary *)data;
                if (self.otaFileData) {
                    CE_SendOtaDataCmd *cmd = [[CE_SendOtaDataCmd alloc]
                        initWithData:self.otaFileData
                        otaStatusInfo:self.cachedOtaStatusInfo
                        deviceInfo:self.cachedDeviceInfo ?: @{}
                        complteProgress:^(CGFloat rate) {
                            int progress = (int)(rate * 100.0);
                            [self fireEvent:@"onOtaProgress" data:@{@"progress": @(progress)}];
                            if (rate >= 1.0) {
                                self.otaFileData = nil;
                            }
                        }];
                    [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
                }
            }
            break;
        }
        case DATA_TYPE_BLE_PAIR_STATUS: {
            if ([data isKindOfClass:[NSDictionary class]]) {
                NSMutableDictionary *p = [NSMutableDictionary dictionaryWithDictionary:data];
                id r = [self pickFirst:data keys:@[@"result", @"value", @"status", @"code"]];
                if (r != nil) p[@"result"] = r;
                [self fireEvent:@"onPairFinish" data:p];
            } else if ([data isKindOfClass:[NSNumber class]]) {
                [self fireEvent:@"onPairFinish" data:@{@"result": data}];
            } else {
                [self fireEvent:@"onPairFinish" data:@{@"result": data ?: @""}];
            }
            break;
        }
        default:
            break;
    }
}

#pragma mark - Exported Methods

UNI_EXPORT_METHOD(@selector(init:))
- (void)init:(UniModuleKeepAliveCallback)callback {
    @try {
        [self registerObserversIfNeeded];
        self.initialized = YES;
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(startScan:))
- (void)startScan:(UniModuleKeepAliveCallback)callback {
    @try {
        CBManagerState state = [CEProductK6 shareInstance].state;
        if (state == CBManagerStatePoweredOff) {
            if (callback) callback([self errResultCode:-100 message:@"bluetooth_off"], NO);
            return;
        }
        if (state == CBManagerStateUnauthorized) {
            if (callback) callback([self errResultCode:-101 message:@"bluetooth_denied"], NO);
            return;
        }
        [self.emittedDevices removeAllObjects];
        [[CEProductK6 shareInstance] startScan];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(stopScan:))
- (void)stopScan:(UniModuleKeepAliveCallback)callback {
    @try {
        [[CEProductK6 shareInstance] stopScan];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(connect:callback:))
- (void)connect:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        CBManagerState state = [CEProductK6 shareInstance].state;
        if (state == CBManagerStatePoweredOff) {
            if (callback) callback([self errResultCode:-100 message:@"bluetooth_off"], NO);
            return;
        }
        NSString *identifier = options[@"identifier"];
        if (!identifier || identifier.length == 0) {
            identifier = options[@"mac"];
        }
        if (!identifier || identifier.length == 0) {
            if (callback) {
                callback([self errResult:@"identifier/mac is required"], NO);
            }
            return;
        }
        CBPeripheral *peripheral = self.peripheralMap[identifier];
        if (!peripheral) {
            if (callback) {
                callback([self errResult:@"Device not found, please scan first"], NO);
            }
            return;
        }
        [[CEProductK6 shareInstance] stopScan];
        [[CEProductK6 shareInstance] connect:peripheral];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(otaUpgrade:callback:))
- (void)otaUpgrade:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        NSString *filePath = nil;
        if ([options isKindOfClass:[NSDictionary class]]) {
            filePath = options[@"filePath"] ?: options[@"path"] ?: options[@"file"];
        }
        if (filePath.length == 0) {
            if (callback) callback([self errResult:@"filePath is required"], NO);
            return;
        }
        NSData *fileData = [NSData dataWithContentsOfFile:filePath];
        if (!fileData) {
            if (callback) callback([self errResult:@"file not found"], NO);
            return;
        }
        self.otaFileData = fileData;
        CE_RequestDevInfoCmd *devCmd = [[CE_RequestDevInfoCmd alloc] init];
        [[CEProductK6 shareInstance] sendCmdToDevice:devCmd complete:nil];
        CE_RequestOTAStatusCmd *otaCmd = [[CE_RequestOTAStatusCmd alloc] init];
        [[CEProductK6 shareInstance] sendCmdToDevice:otaCmd complete:nil];
        if (callback) callback([self okResult:nil], NO);
    } @catch (NSException *exception) {
        if (callback) callback([self errResult:exception.reason], NO);
    }
}

UNI_EXPORT_METHOD(@selector(disconnect:))
- (void)disconnect:(UniModuleKeepAliveCallback)callback {
    @try {
        [CE_SensorCmd close];
        [[CEProductK6 shareInstance] stopScan];
        [[CEProductK6 shareInstance] cleanCmdQueue];
        [[CEProductK6 shareInstance].connect cancel];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(unbind:))
- (void)unbind:(UniModuleKeepAliveCallback)callback {
    @try {
        [CE_SensorCmd close];
        [[CEProductK6 shareInstance] stopScan];
        [[CEProductK6 shareInstance] cleanCmdQueue];
        [[CEProductK6 shareInstance] releaseBind];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(isConnected:))
- (void)isConnected:(UniModuleKeepAliveCallback)callback {
    @try {
        ProductStatus status = [CEProductK6 shareInstance].status;
        BOOL connected = (status == ProductStatus_connected || status == ProductStatus_completed);
        if (callback) {
            callback([self okResult:@{@"connected": @(connected)}], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(devicePairFinish:callback:))
- (void)devicePairFinish:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        CE_SyncPairOKCmd *cmd = [[CE_SyncPairOKCmd alloc] init];
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(sendSyncInfo:))
- (void)sendSyncInfo:(UniModuleKeepAliveCallback)callback {
    @try {
        CE_SyncPairOKCmd *pairCmd = [[CE_SyncPairOKCmd alloc] init];
        CE_SyncGoalCmd *goalCmd = [[CE_SyncGoalCmd alloc] initWithStep:8000 distance:5000 calories:300 sleeptime:480 sportTime:30];
        YD_SyncLanguageCmd *langCmd = [[YD_SyncLanguageCmd alloc] initWithLanguage:0];
        CE_SyncHybridCmd *hybrid = [[CE_SyncHybridCmd alloc] initWithItems:@[pairCmd, goalCmd, langCmd]];
        [[CEProductK6 shareInstance] sendCmdToDevice:hybrid complete:nil];
        CE_RequestAllInfoCmd *requestCmd = [[CE_RequestAllInfoCmd alloc] init];
        [[CEProductK6 shareInstance] sendCmdToDevice:requestCmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(syncTime:))
- (void)syncTime:(UniModuleKeepAliveCallback)callback {
    @try {
        NSDate *now = [NSDate date];
        NSTimeInterval absTime = [now timeIntervalSince1970];
        NSInteger offset = [[NSTimeZone localTimeZone] secondsFromGMTForDate:now];
        CE_SyncTimeCmd *cmd = [[CE_SyncTimeCmd alloc] initWithAbsTime:absTime
                                                               offset:offset
                                                               format:1
                                                             mdFormat:1];
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:@{@"absTime": @((long long)absTime)}], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(setHeartRateSwitch:callback:))
- (void)setHeartRateSwitch:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        BOOL open = options[@"open"] == nil ? YES : [options[@"open"] boolValue];
        CE_SyncHeartRateCmd *cmd = [[CE_SyncHeartRateCmd alloc] init];
        cmd.status = open ? 1 : 0;
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(setHRVSwitch:callback:))
- (void)setHRVSwitch:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        BOOL open = options[@"open"] == nil ? YES : [options[@"open"] boolValue];
        CE_SyncHRVCmd *cmd = [[CE_SyncHRVCmd alloc] init];
        cmd.status = open ? 1 : 0;
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(setBloodOxygenSwitch:callback:))
- (void)setBloodOxygenSwitch:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        BOOL open = options[@"open"] == nil ? YES : [options[@"open"] boolValue];
        CE_SyncHeartO2Cmd *cmd = [[CE_SyncHeartO2Cmd alloc] init];
        cmd.status = open ? 1 : 0;
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(setHeartAutoSwitch:callback:))
- (void)setHeartAutoSwitch:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        BOOL heart = options[@"heart"] == nil ? YES : [options[@"heart"] boolValue];
        BOOL o2 = options[@"o2"] == nil ? YES : [options[@"o2"] boolValue];
        YD_SyncAutoHeartCmd *cmd = [[YD_SyncAutoHeartCmd alloc] init];
        cmd.onoff = heart ? 1 : 0;
        // 官方 Demo 只下发 onoff。戒指不支持 24 小时心率时，把 hr24hOnoff/time 置 1
        // 会让设备侧关闭普通自动心率，导致 iOS 只有血氧/HRV、没有心率。
        cmd.hr24hOnoff = 0;
        cmd.time = 0;
        cmd.O2_onoff = o2 ? 1 : 0;
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(setSensorOn:callback:))
- (void)setSensorOn:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        BOOL open = options[@"open"] == nil ? YES : [options[@"open"] boolValue];
        CE_SensorCmd *cmd = [[CE_SensorCmd alloc] initWith:open ? 1 : 0];
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(syncUserInfo:callback:))
- (void)syncUserInfo:(NSDictionary *)options callback:(UniModuleKeepAliveCallback)callback {
    @try {
        CE_SyncUserInfoCmd *cmd = [[CE_SyncUserInfoCmd alloc] init];
        cmd.userId = (uint32_t)MAX(0, [options[@"userId"] longLongValue]);
        cmd.sex = (uint8_t)([options[@"sex"] integerValue] == 1 ? 1 : 0);
        cmd.age = (uint8_t)MIN(120, MAX(1, [options[@"age"] integerValue]));
        cmd.height = (uint8_t)MIN(250, MAX(50, [options[@"height"] integerValue]));
        cmd.weight = (uint8_t)MIN(250, MAX(20, [options[@"weight"] integerValue]));
        cmd.lrHand = (uint8_t)([options[@"lrHand"] integerValue] == 0 ? 0 : 1);
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(pullCachedHealthEvents:))
- (void)pullCachedHealthEvents:(UniModuleKeepAliveCallback)callback {
    NSArray *events = @[];
    @synchronized (self.healthCache) {
        events = [self.healthCache copy];
        [self.healthCache removeAllObjects];
    }
    if (callback) {
        callback([self okResult:@{@"events": events}], NO);
    }
}

UNI_EXPORT_METHOD(@selector(requestBattery:))
- (void)requestBattery:(UniModuleKeepAliveCallback)callback {
    @try {
        CE_RequestBatteryCmd *cmd = [[CE_RequestBatteryCmd alloc] init];
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(requestDeviceInfo:))
- (void)requestDeviceInfo:(UniModuleKeepAliveCallback)callback {
    @try {
        CE_RequestDevInfoCmd *cmd = [[CE_RequestDevInfoCmd alloc] init];
        [[CEProductK6 shareInstance] sendCmdToDevice:cmd complete:nil];
        if (callback) {
            callback([self okResult:nil], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

UNI_EXPORT_METHOD(@selector(reconnect:))
- (void)reconnect:(UniModuleKeepAliveCallback)callback {
    @try {
        [[CEProductK6 shareInstance] startAutoConnect];
        if (callback) {
            callback([self okResult:@{@"started": @YES}], NO);
        }
    } @catch (NSException *exception) {
        if (callback) {
            callback([self errResult:exception.reason], NO);
        }
    }
}

#pragma mark - Bluetooth Permission (iOS)

- (NSString *)btAuthString:(CBManagerAuthorization)auth {
    if (@available(iOS 13.1, *)) {
        switch (auth) {
            case CBManagerAuthorizationNotDetermined: return @"notDetermined";
            case CBManagerAuthorizationRestricted: return @"restricted";
            case CBManagerAuthorizationDenied: return @"denied";
            case CBManagerAuthorizationAllowedAlways: return @"allowedAlways";
            default: break;
        }
    }
    return @"unknown";
}

- (void)finishPerm:(NSDictionary *)data {
    if (!self.permCallback) return;
    UniModuleKeepAliveCallback cb = self.permCallback;
    self.permCallback = nil;
    self.permCentral = nil;
    cb([self okResult:data], NO);
}

UNI_EXPORT_METHOD(@selector(checkBluetoothPermission:))
- (void)checkBluetoothPermission:(UniModuleKeepAliveCallback)callback {
    NSString *authStr = @"unknown";
    if (@available(iOS 13.1, *)) {
        authStr = [self btAuthString:CBCentralManager.authorization];
    }
    // 用一个常驻的 central 读取真实 state。之前依赖 permCentral，
    // 但它在 finishPerm 里会被置 nil，导致这里永远返回 Unknown(0)，
    // 被 JS 误判成“蓝牙未开启”。
    if (!self.stateCentral) {
        NSDictionary *opts = @{CBCentralManagerOptionShowPowerAlertKey: @NO};
        self.stateCentral = [[CBCentralManager alloc] initWithDelegate:self queue:dispatch_get_main_queue() options:opts];
    }
    CBManagerState st = self.stateCentral.state;
    if (callback) {
        callback([self okResult:@{@"authorization": authStr, @"state": @(st)}], NO);
    }
}

UNI_EXPORT_METHOD(@selector(requestBluetoothPermission:))
- (void)requestBluetoothPermission:(UniModuleKeepAliveCallback)callback {
    if (@available(iOS 13.1, *)) {
        CBManagerAuthorization auth = CBCentralManager.authorization;
        if (auth == CBManagerAuthorizationAllowedAlways) {
            if (callback) callback([self okResult:@{@"authorization": @"allowedAlways", @"granted": @YES}], NO);
            return;
        }
        if (auth == CBManagerAuthorizationDenied || auth == CBManagerAuthorizationRestricted) {
            if (callback) callback([self errResult:@"蓝牙权限被拒绝，请前往 设置 > 本App 开启蓝牙权限"], NO);
            return;
        }
    }
    self.permCallback = callback;
    if (!self.permCentral) {
        NSDictionary *opts = @{CBCentralManagerOptionShowPowerAlertKey: @YES};
        self.permCentral = [[CBCentralManager alloc] initWithDelegate:self queue:dispatch_get_main_queue() options:opts];
    }
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(15 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        if (self.permCallback) {
            [self finishPerm:@{@"granted": @NO, @"timeout": @YES}];
        }
    });
}

- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    if (central != self.permCentral || !self.permCallback) return;
    if (central.state == CBManagerStateUnknown || central.state == CBManagerStateResetting) return;
    NSString *authStr = @"unknown";
    if (@available(iOS 13.1, *)) {
        authStr = [self btAuthString:CBCentralManager.authorization];
    }
    BOOL granted = (central.state == CBManagerStatePoweredOn);
    if (@available(iOS 13.1, *)) {
        granted = granted && (CBCentralManager.authorization == CBManagerAuthorizationAllowedAlways);
    }
    [self finishPerm:@{@"granted": @(granted), @"state": @(central.state), @"authorization": authStr}];
}

UNI_EXPORT_METHOD(@selector(openAppSettings:))
- (void)openAppSettings:(UniModuleKeepAliveCallback)callback {
    dispatch_async(dispatch_get_main_queue(), ^{
        NSURL *url = [NSURL URLWithString:UIApplicationOpenSettingsURLString];
        if (url && [[UIApplication sharedApplication] canOpenURL:url]) {
            if (@available(iOS 10.0, *)) {
                [[UIApplication sharedApplication] openURL:url options:@{} completionHandler:nil];
            } else {
                [[UIApplication sharedApplication] openURL:url];
            }
        }
        if (callback) callback([self okResult:nil], NO);
    });
}

UNI_EXPORT_METHOD(@selector(openBluetoothSettings:))
- (void)openBluetoothSettings:(UniModuleKeepAliveCallback)callback {
    [self openAppSettings:callback];
}

@end
