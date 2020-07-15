# 화상 채팅 overlay 개발 과정에서의 이슈

1. 일부 디바이스에서 ```example.page.link```(dynamic link에 사용되는 도메인)을 url로 인식을 못해서 추가적으로 단축 url 서비스를 사용해야 했음
2. resolution/bitrate/fps 관련
  
     - Resolution:  Video call 에서는 원격 video에 대해 1280x720 이상의 해상도는 지원하지 않는 듯 보임. [문서](https://docs.agora.io/en/faqs/API%20Reference/java/v2.4/classio_1_1agora_1_1rtc_1_1video_1_1_video_encoder_configuration.html)를 보면 ```Whether 720p+ can be supported depends on the device. If the device cannot support 720p, the frame rate will be lower than the one listed in the table.``` 라고 하지만, agora android sdk ```2.4.1``` 버전 부터 ```VideoDimensions```의 static 변수에서 1280x720이 마지막이다(그 전 버전에는 3840x2160 까지 존재했다). 또한 실제로 ```VideoEncoderConfiguration.VideoDimensions```의 값을 늘려봤는데 효과가 없었다.
     - Bitrate: ```engine.setVideoEncoderConfiguration()```의 파라미터인 ```VideoEncoderConfiguration```로 설정 가능한데, 정확하게 효과가 있는지는 확인하지 못함. 문서 상에서는 추천 값만 사용하라고 나와 있음.
     - Fps: 조사 x
3. 원형 화상 채팅 overlay UI 관련

    - 기본적으로 View의 corner를 원형으로 바꾸는 방법은 ```gradientDrawable.setCornerRadius()``` 호출 후, 해당 drawable을 ```view.setBackground(gradientDrawable)```로 적용하면 된다. 그런데 SurfaceView(화상 채팅 view)은 shape을 곧바로 적용할 수 없다(https://stackoverflow.com/a/36583173/11658022).
    - 이를 우회하려면 SurfaceView를 FrameLayout이나 RelativeLayout 안에 위치 시킨 후, SurfaceView를 투명한 View를 이용해 z축으로 덮어 쓴 후, 투명한 View의 corner를 조절하면 된다. 그런데 현재 코드 상 이것도 곧바로 적용할 수 없다. 현재 채팅 화면이 사실 여러 개의 SurfaceView로 이루어진 RecyclerView 이기 때문이다.
      - 일단 각 채팅 화면을 모두 둥글게 할 필요도 없음. 그럼에도 그렇게 할거라면 runtime에 생성 되는 SurfaceView 마다 FrameLayout으로 감싸주는 작업 필요할 것으로 보임.
      - 그렇다면 RecyclerView만 둥글게 하면 되는데, setBackground를 활용하는 방식이 이유는 모르겠으나 적용이 안됨.
    - 결론: 더 많은 조사 필요

# Open Video Call for Android

*English | [中文](README.zh.md)*

The Open Video Call for Android Sample App is an open-source demo that will help you get video chat integrated directly into your Android applications using the Agora Video SDK.

With this sample app, you can:
- Join / leave channel
- Mute / unmute audio
- Enable / disable video
- Switch camera
- Setup resolution, frame rate and bit rate
- Enable encryption
- Enable beautify filter

## Prerequisites

- Android Studio 3.3 or above
- Real devices (Nexus 5X or other devices)
- Some simulators are function missing or have performance issue, so real device is the best choice

## Quick Start

This section shows you how to prepare, build, and run the sample application.

### Obtain an App ID

To build and run the sample application, get an App ID:
1. Create a developer account at [agora.io](https://dashboard.agora.io/signin/). Once you finish the signup process, you will be redirected to the Dashboard.
2. Navigate in the Dashboard tree on the left to **Projects** > **Project List**.
3. Save the **App ID** from the Dashboard for later use.
4. Generate a temp **Access Token** (valid for 24 hours) from dashboard page with given channel name, save for later use.

5. Update "app/src/main/res/values/strings_config.xml" with your App ID and Token.
```
<string name="private_app_id"><#YOUR APP ID#></string>
<!-- Please leave it if not enable App Certificate -->
<!-- You can generate a temporary token at https://dashboard.agora.io/projects -->
<string name="agora_access_token"><#YOUR TOKEN#></string>
```

### Integrate the Agora Video SDK

The SDK must be integrated into the sample project before it can opened and built. There are two methods for integrating the Agora Video SDK into the sample project. The first method uses JCenter to automatically integrate the SDK files. The second method requires you to manually copy the SDK files to the project.

#### Method 1 - Integrate the SDK Automatically Using JCenter (Recommended)

1. Clone this repository.
2. Open **app/build.gradle** and add the following line to the `dependencies` list:

  ```
  ...
  dependencies {
      ...
      implementation 'io.agora.rtc:full-sdk:3.0.0'
  }
  ```

#### Method 2 - Manually copy the SDK files

1. Download the Agora Video SDK from [Agora.io SDK](https://www.agora.io/en/download/).
2. Unzip the downloaded SDK package.
3. Copy the following files from from the **libs** folder of the downloaded SDK package:

Copy from SDK|Copy to Project Folder
---|---
.jar file|**/apps/libs** folder
**arm64-v8a** folder|**/app/src/main/jniLibs** folder
**x86** folder|**/app/src/main/jniLibs** folder
**armeabi-v7a** folder|**/app/src/main/jniLibs** folder

    

### Run the Application

Open project with Android Studio, connect your Android device, build and run.
      
Or use `Gradle` to build and run.


## Resources

- For potential issues, take a look at our [FAQ](https://docs.agora.io/cn/faq) first
- Dive into [Agora SDK Samples](https://github.com/AgoraIO) to see more tutorials
- Take a look at [Agora Use Case](https://github.com/AgoraIO-usecase) for more complicated real use case
- Repositories managed by developer communities can be found at [Agora Community](https://github.com/AgoraIO-Community)
- You can find full API documentation at [Document Center](https://docs.agora.io/en/)
- If you encounter problems during integration, you can ask question in [Stack Overflow](https://stackoverflow.com/questions/tagged/agora.io)
- You can file bugs about this sample at [issue](https://github.com/AgoraIO/Basic-Video-Call/issues)

## License

The MIT License (MIT)
