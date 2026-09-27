#import "CoolWearBLEModule.h"
#import <BluetoothLibrary/BluetoothLibrary.h>

static NSString * const kEventChannel = @"CoolWearBLE";

@interface CoolWearBLEModule ()

@property (nonatomic, assign) BOOL initialized;
@property (nonatomic, assign) BOOL observersRegistered;
@property (nonatomic, strong) NSMutableDictionary<NSString *, CBPeripheral *> *peripheralMap;
@property (nonatomic, strong) NSMutableSet<NSString *> *emittedDevices;

@end

@implementation CoolWearBLEModule

- (instancetype)init {
    self = [super init];
    if (self) {
        _peripheralMap = [NSMutableDictionary dictionary];
        _emittedDevices = [NSMutableSet set];
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
            if ([data isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = (NSDictionary *)data;
                NSMutableDictionary *payload = [NSMutableDictionary dictionary];
                id level = dict[@"level"] ?: dict[@"battery"] ?: dict[@"Battery"] ?: dict[@"power"];
                id charging = dict[@"charging"] ?: dict[@"chargerStatus"] ?: dict[@"isChargerStatus"] ?: dict[@"isCharging"] ?: dict[@"deviceChargerStatus"];
                if (level != nil) {
                    payload[@"level"] = @([level intValue]);
                }
                BOOL isCharging = NO;
                if ([charging respondsToSelector:@selector(boolValue)]) {
                    isCharging = [charging boolValue];
                } else if ([charging respondsToSelector:@selector(intValue)]) {
                    isCharging = [charging intValue] != 0;
                }
                payload[@"charging"] = @(isCharging);
                payload[@"chargerStatus"] = @(isCharging);
                payload[@"isChargerStatus"] = @(isCharging);
                payload[@"raw"] = dict;
                [self fireEvent:@"onBattery" data:payload];
            }
            break;
        }
        case DATA_TYPE_REAL_HEART:
        case DATA_TYPE_EXERCISE_HEART:
        case DATA_TYPE_REAL_HR: {
            [self fireEvent:@"onHeartRate" data:data ?: @[]];
            break;
        }
        case DATA_TYPE_REAL_HRV:
        case DATA_TYPE_HISTORY_HRV: {
            [self fireEvent:@"onHRV" data:data ?: @[]];
            break;
        }
        case DATA_TYPE_HISTORY_TEMP: {
            [self fireEvent:@"onTemperature" data:data ?: @[]];
            break;
        }
        case DATA_TYPE_SLEEP: {
            [self fireEvent:@"onSleep" data:data ?: @{}];
            break;
        }
        case DATA_TYPE_REAL_O2:
        case DATA_TYPE_HISTORY_O2: {
            [self fireEvent:@"onBloodOxygen" data:data ?: @[]];
            break;
        }
        case DATA_TYPE_REAL_SPORT:
        case DATA_TYPE_HISTORY_SPORT: {
            [self fireEvent:@"onSport" data:data ?: @[]];
            break;
        }
        case DATA_TYPE_MIX_SPORT: {
            [self fireEvent:@"onMixSport" data:data ?: @[]];
            break;
        }
        case DATA_TYPE_DEVINFO: {
            if ([data isKindOfClass:[NSDictionary class]]) {
                NSDictionary *dict = (NSDictionary *)data;
                NSMutableDictionary *payload = [NSMutableDictionary dictionaryWithDictionary:dict];
                id ver = dict[@"softwareVer"] ?: dict[@"firmware"] ?: dict[@"SoftwareVer"] ?: dict[@"softwareVersion"] ?: dict[@"version"];
                if (ver != nil) {
                    NSString *verStr = [NSString stringWithFormat:@"%@", ver];
                    payload[@"softwareVer"] = verStr;
                    payload[@"firmware"] = verStr;
                }
                id hw = dict[@"hardwareVer"] ?: dict[@"HardwareVer"] ?: dict[@"hardwareVersion"];
                if (hw != nil) {
                    payload[@"hardwareVer"] = [NSString stringWithFormat:@"%@", hw];
                }
                [self fireEvent:@"onDeviceInfo" data:payload];
            } else {
                [self fireEvent:@"onDeviceInfo" data:data ?: @{}];
            }
            break;
        }
        case DATA_TYPE_BLE_PAIR_STATUS: {
            [self fireEvent:@"onPairFinish" data:data ?: @{}];
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

UNI_EXPORT_METHOD(@selector(disconnect:))
- (void)disconnect:(UniModuleKeepAliveCallback)callback {
    @try {
        [CE_SensorCmd close];
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
        CE_RequestAllInfoCmd *cmd = [[CE_RequestAllInfoCmd alloc] init];
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
        NSInteger interval = options[@"intervalMin"] ? [options[@"intervalMin"] integerValue] : 30;
        if (interval <= 0) {
            interval = 30;
        }
        YD_SyncAutoHeartCmd *cmd = [[YD_SyncAutoHeartCmd alloc] init];
        cmd.onoff = heart ? 1 : 0;
        cmd.O2_onoff = o2 ? 1 : 0;
        cmd.time = (Byte)interval;
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

@end
