#import <Foundation/Foundation.h>

@class DCUniSDKInstance;

@interface DCUniSDKInstance : NSObject

- (void)fireGlobalEvent:(NSString *)eventName params:(NSDictionary *)params;

@end

@interface DCUniModule : NSObject

@property (nonatomic, strong) dispatch_queue_t uniExecuteQueue;
@property (nonatomic, strong) NSThread *uniExecuteThread;
@property (nonatomic, weak) DCUniSDKInstance *uniInstance;

@end

#define UNI_EXPORT_METHOD(method) UNI_EXPORT_METHOD_INTERNAL(method,wx_export_method_)

#define UNI_EXPORT_METHOD_SYNC(method) UNI_EXPORT_METHOD_INTERNAL(method,wx_export_method_sync_)

#define UNI_EXPORT_METHOD_INTERNAL(method, token) \
+ (NSString *)UNI_CONCAT_WRAPPER(token, __LINE__) { \
    return NSStringFromSelector(method); \
}

#define UNI_CONCAT_WRAPPER(a, b)    UNI_CONCAT(a, b)

#define UNI_CONCAT(a, b)   a ## b


typedef void (^UniModuleKeepAliveCallback)(id result, BOOL keepAlive);
