# R8 is used for release builds. Debug CI does not minify.
-keep class com.marcopt74.damelume.system.alarm.TimerAlarmReceiver { *; }
-keep class com.marcopt74.damelume.system.notify.NotificationActionReceiver { *; }
-keep class com.marcopt74.damelume.system.boot.BootReceiver { *; }
