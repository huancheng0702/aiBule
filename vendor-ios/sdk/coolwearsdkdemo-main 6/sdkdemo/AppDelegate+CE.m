//
//  AppDelegate+CE.m
//  Daewoo
//
//  Created by LiJie on 2017/1/11.
//  Copyright © 2017年 celink. All rights reserved.
//

#import "AppDelegate+CE.h"

static NSString *CEHexStringFromData(NSData *data) {
    if (data.length == 0) {
        return @"";
    }
    
    const unsigned char *bytes = data.bytes;
    NSMutableString *hex = [NSMutableString stringWithCapacity:data.length * 3];
    for (NSUInteger index = 0; index < data.length; index++) {
        [hex appendFormat:@"%02X", bytes[index]];
        if (index < data.length - 1) {
            [hex appendString:@" "];
        }
    }
    return hex;
}

@implementation AppDelegate (CE)

#pragma mark -  Public
- (void)setupDefaultConfigure {
    
    //    [AMapServices sharedServices].apiKey = @"335c1964e927f55e5e980f9148ba0687";
//    // 设置网络请求的配置
//    [[AFNetworkReachabilityManager sharedManager] startMonitoring];
//    [Network startListening];
//    [CERequestService configeWithuserInfo];
//    // 苹果健康初始化
//    [[AppleHealthCenter shareHealthyInstance] getAppleHealthSteps]
    
    __weak CEProductK6 *productK6 = [CEProductK6 shareInstance];
    productK6.sendOriginalDataHandler = ^(NSData *data) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [NSNotificationCenter.defaultCenter postNotificationName:@"CEOriginalDataLog"
                                                              object:nil
                                                            userInfo:@{@"direction": @"RAW SEND",
                                                                       @"data": CEHexStringFromData(data)}];
        });
    };
    productK6.receiveOriginalDataHandler = ^(NSData *data) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [NSNotificationCenter.defaultCenter postNotificationName:@"CEOriginalDataLog"
                                                              object:nil
                                                            userInfo:@{@"direction": @"RAW RECV",
                                                                       @"data": CEHexStringFromData(data)}];
        });
    };
    productK6.connectStatusChanged = ^(ProductStatus status) {
        NSString *statusString = @"unknow status";
        switch (status) {
            case ProductStatus_none:
                statusString = @"none";
                break;
            case ProductStatus_powerOff:
                statusString = @"powerOff";
                break;
            case ProductStatus_searching:
                statusString = @"searching";
                break;
            case ProductStatus_connecting:
                statusString = @"connecting";
                break;
            case ProductStatus_disconnected:
                statusString = @"disconnected";
                break;
            case ProductStatus_connected:
                statusString = @"connected";
                break;
            case ProductStatus_completed:
                statusString = @"completed";
                break;
            default:
                break;
        }
//        DDLogInfo(@"[status] uuid:%@ status:%@ value:%li", productK6.lastConnectUUId, statusString, (long)status);
    };
}

- (BOOL)customApplication:(UIApplication *)application OpenURL:(NSURL *)url options:(NSDictionary<NSString *,id> *)options{
    return YES;
}

@end
