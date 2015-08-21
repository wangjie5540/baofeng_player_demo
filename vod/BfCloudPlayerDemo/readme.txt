发布说明
--------

请按以下步骤生成最终发布版 (运行期不输出日志) 的APK:

1. 去掉“Build Automatically”前的勾。

2. 执行 Clean，再 Build All。

3. 在项目 BfCloudPlayerDemo 上右键选择 Android Tools -> Export Signed Application Package。

4. 选择 Use existing keystore。
   Location 选择 BfCloudPlayerDemo 项目根目录下的 keystore 文件。
   密码为 123456。

5. 最终生成 BfCloudPlayerDemo.apk 。
