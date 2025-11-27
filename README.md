# Dice for Fate 项目说明

## 项目概览
- 离线随机骰子应用，支持固定骰型与自定义 `mdn` 输入（m∈[2,10]，n∈[2,100]）。
- 主界面居中显示掷骰结果；m=1 显示单值，m≥2 显示“结果列表 + 总和”。
- 历史记录在二级界面展示，支持清空；结果与总和字号可分别调节并本地保存。

## 根目录文件说明
- `settings.gradle`：Gradle 子模块声明（包含 `:app`）。
- `build.gradle`：项目级构建脚本与仓库配置。
- `gradle.properties`：构建属性（启用 AndroidX、Jetifier、JVM 参数等）。
- `gradlew` / `gradlew.bat`：Gradle Wrapper 启动脚本，保证可复现构建。
- `gradle/wrapper/gradle-wrapper.jar` / `gradle/wrapper/gradle-wrapper.properties`：Wrapper 配置与二进制。

## 交付与归档
- `dist/DiceForFate-v1.4.0-debug.apk`：最新调试版 APK。
- `dist/checksums.txt`：对应 APK 的 SHA-256 校验值。
- `archive/DiceForFate-v1.4.0-debug.zip`：发行包压缩文件（APK+校验）。
- `archive/DiceForFate-source-v1.4.0.zip`：源码归档压缩文件。

## app 模块
- `app/build.gradle`：应用模块构建配置与依赖；启用 ViewBinding、Kotlin、AndroidX 等。
- `app/proguard-rules.pro`：混淆规则（当前为空，可按需扩展）。

### Android 清单
- `app/src/main/AndroidManifest.xml`：应用清单，注册入口 `MainActivity` 与 `HistoryActivity`，声明震动权限与主题等。

### 源码目录 `app/src/main/java/com/example/dice/`
- `DiceRoller.kt`：使用 `SecureRandom` 实现掷骰核心逻辑，生成均匀分布的点数与总和。
- `HistoryStore.kt`：会话级历史记录共享仓库（`MutableLiveData<List<RollResult>>`），供多个界面订阅与更新。
- `MainViewModel.kt`：触发掷骰，写入历史，输出当前结果供主界面展示。
- `MainActivity.kt`：主界面逻辑：
  - 固定骰型按钮与 `mdn` 对话框（范围提示、动态校验、非法禁用“确定”）。
  - 结果居中显示，两行文本（结果列表与总和）按 m 值切换显示。
  - 结果与总和字号滑条，实时生效并持久化到本地。
- `HistoryActivity.kt`：历史记录二级界面，使用 RecyclerView 展示并支持清空。
- `ResultFormatter.kt`：结果文本格式化；m=1 返回单值，m≥2 返回两行“结果/总和”。
- `model/Dice.kt`：骰子模型（`count=m`，`sides=n`）。
- `model/RollResult.kt`：掷骰结果数据结构（骰型、所有点数、总和、时间戳）。
- `ui/ResultAdapter.kt`：历史记录列表适配器，渲染每条记录的骰型、结果、总和与时间。

### 资源目录 `app/src/main/res/`
- `layout/activity_main.xml`：主界面布局，包含按钮网格、结果两行显示区、字号调节滑条与历史入口。
- `layout/activity_history.xml`：历史界面布局，包含 RecyclerView 与清空按钮。
- `layout/item_roll_result.xml`：历史记录列表项布局。
- `values/strings.xml`：字符串资源（应用名、提示文案、范围说明、按钮文本等）。
- `values/colors.xml`：颜色配置（主题色与图标底色）。
- `values/themes.xml`：主题样式（`Theme.MaterialComponents.DayNight.NoActionBar`）。
- `values/ic_launcher_background.xml`、`drawable/ic_launcher_foreground.xml`、`mipmap-anydpi-v26/*`：应用图标资源。

### 测试目录 `app/src/test/java/com/example/dice/`
- `DiceRollerTest.kt`：掷骰结果范围与粗略分布测试（均匀性基本检查）。
- `ResultFormatterTest.kt`：结果文本格式化测试（m=1 单值、m≥2 两行）。

## 构建与安装
- 构建：在已安装 Android SDK 的环境下执行 `./gradlew assembleDebug` 生成 APK。
- 安装：
  - 直接在手机上打开 APK 安装；或
  - 使用 ADB：`adb install -r dist/DiceForFate-v1.4.0-debug.apk`。

## 功能要点
- 固定骰型：`1d2 1d3 1d4 1d6 1d10 1d20 1d100 2d2 2d3 2d4 2d6`。
- 自定义 `mdn`：m∈[2,10]、n∈[2,100]；非法或为空时禁用“确定”并提示“输入数字不合法”。
- 显示规则：m=1 仅显示单个结果；m≥2 显示所有点数与总和（两行）。
- 字号调节：结果与总和的字号滑条独立，设置持久化到本地。
- 历史记录：二级界面列表展示，支持清空。

## 备注
- 项目不依赖网络，所有随机均在本地使用 `SecureRandom` 生成。
- `tools/gradle-8.1.1/` 为本地临时工具残留，归档时可忽略，不参与构建。
