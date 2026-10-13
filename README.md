# Bundle Tab

为 Minecraft 原版配方书添加收纳袋页签。在背包或工作台中点击绿色书本，切换到收纳袋页签，再点击对应收纳袋查看内容。

支持物品名称、拼音和物品 ID 搜索，以及袋内物品分页。使用原版物品栏操作与服务器同步，无需安装服务端模组。

## 环境

- Minecraft 26.3.x
- Fabric Loader 0.19.5 或更新版本
- Java 25 或更新版本（构建需要 JDK 25）

## 操作

| 操作 | 行为 |
| --- | --- |
| 左键点击收纳袋图标 | 查看袋内物品 |
| 空手左键点击袋内物品 | 取出选中的整组物品 |
| 手持物品左键点击袋内区域 | 存入物品，超出容量的部分留在手上 |
| Shift 点击袋内物品 | 转移到背包，无法容纳的部分留在袋内 |
| 空格加左键点击背包物品 | 存入选中的收纳袋；未选择时使用第一个可容纳的收纳袋 |

当前版本未接入 Mouse Tweaks 的袋内拖拽和滚轮手势。完整模组组合的兼容性仍需实测。

## 构建与安装

Windows PowerShell：

```powershell
.\gradlew.bat build
```

Linux 或 macOS：

```sh
./gradlew build
```

将 `build/libs/` 中不带 `-sources` 后缀的 `bundle-tab-*.jar` 放入客户端的 `mods` 目录，然后重启游戏。

Bundle Tab 沿用 `better-bundle` 模组 ID。安装前移除旧版 Better Bundle 或 Bundle Tab jar，避免重复加载。

## 游戏测试

测试需要可用的图形界面。默认使用 Mouse Tweaks 2.31 检查原版背包拖拽，同时验证配方书页签、搜索、物品转移、满背包和窗口尺寸变化。

```powershell
.\gradlew.bat runClientGameTest
.\gradlew.bat runClientGameTest -PmouseTweaksTest=false
```

## 来源与许可

基于 [zeroowo24/BetterBundleHUDMod](https://github.com/zeroowo24/BetterBundleHUDMod) 开发，保留上游提交历史。当前维护者为 [PluginsKers](https://github.com/PluginsKers)。

项目沿用 [CC0-1.0](LICENSE)。内嵌的 pinyin4j 使用其独立许可证。
