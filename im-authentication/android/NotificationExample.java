// function to show Notification in java


public void showIPNotification(Context context, String notificationTitle, String messageBody, int icNotification) {
      // Register IMVerificationActivity
      Class<?> accessClass = IMVerificationActivity.class;
      

      // Create intent for the notification
      Intent intent = new Intent(context, accessClass);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
      
      PendingIntent pendingIntent;
      // Update for Android 12 and later
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          pendingIntent = PendingIntent.getActivity(
                  context,
                  IPConfiguration.getInstance().getREQUEST_CODE(), // IPification Request code
                  intent,
                  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
          );
      } else {
          pendingIntent = PendingIntent.getActivity(
                  context,
                  IPConfiguration.getInstance().getREQUEST_CODE(), // IPification Request code
                  intent,
                  PendingIntent.FLAG_UPDATE_CURRENT
          );
      }

      // Notification channel ID and name
      String notificationChannelId = "ip_notification_cid";
      String notificationChannelName = "ip_notification";

      // Notification sound
      Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

      // Build the notification
      NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, notificationChannelId)
              .setSmallIcon(icNotification)
              .setContentTitle(notificationTitle)
              .setContentText(messageBody)
              .setAutoCancel(true)
              .setSound(defaultSoundUri)
              .setPriority(Notification.PRIORITY_MAX)
              .setVibrate(new long[0])
              .setContentIntent(pendingIntent);

      NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

      // Create notification channel for Android Oreo and later
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          NotificationChannel channel = new NotificationChannel(
                  notificationChannelId,
                  notificationChannelName,
                  NotificationManager.IMPORTANCE_HIGH
          );
          channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
          channel.enableVibration(true);
          notificationManager.createNotificationChannel(channel);
      }

      // Show the notification
      notificationManager.notify(IPConfiguration.getInstance().getNOTIFICATION_ID(), notificationBuilder.build());
  }
