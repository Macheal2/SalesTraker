LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-telephony-common
LOCAL_JAVA_LIBRARIES += mediatek-framework
LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := SalesTraker
LOCAL_CERTIFICATE := platform

LOCAL_PRIVATE_PLATFORM_APIS := true

include $(BUILD_PACKAGE)
