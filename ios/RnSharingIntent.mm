#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(RnSharingIntent, NSObject)

RCT_EXTERN_METHOD(getFileNames:(NSString)url
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter: (RCTPromiseRejectBlock)reject);

RCT_EXTERN_METHOD(clearFileNames)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
