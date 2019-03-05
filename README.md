# navigation_tcp_android

Android app acting as TCP server and transmitting compass and location data.

Can set port number, location update interval and accuracy of location through Settings.

Configured to transmit data even when the app is background or the screen is off by implementing long on going Service.

Showcasing how to handle permissions, how to request the dialog to enable location like Google Maps, and how to handle GPS status depending on the Android API level. In this app, fused location from google play services is used to offer more accurate location positions.

For the compass data, the app is transmitting yaw (azimuth), pitch and roll in Euler's angles.

Since the apps needs to use magnetometer to define direction (azimuth), it is advised not use to use the app near strong magnetic field.
