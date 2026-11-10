# IDEA 插件自动更新

在 IDEA 中配置一次，即可通过自定义插件仓库检查本插件的后续版本。

1. 打开 Settings → Plugins。
2. 点击齿轮 → Manage Plugin Repositories。
3. 点击 +，添加以下地址并保存：


    https://raw.githubusercontent.com/sharkfly666/ai-generator-commit-message/main/updatePlugins.xml


已从磁盘安装的同 ID 插件也可以使用此更新源。IDEA 会按更新设置检查新版本，提供 Update 按钮；不会自动无提示安装。需要能够访问 raw.githubusercontent.com 和 GitHub Release 下载地址。

## 发版自动化

推送版本标签（如 v1.2.2）后，现有发布工作流会：

1. 构建插件并验证兼容性。
2. 从 ZIP 中的 META-INF/plugin.xml 提取 ID、版本与 IDE 构建范围，核对标签与插件版本一致。
3. 创建 GitHub Release 并上传 ZIP。
4. 将生成的 updatePlugins.xml 提交到默认分支；内容相同时跳过提交。

生成器仅使用 Python 标准库。手动生成示例：

    python3 scripts/generate_update_plugins.py build/distributions/ai-generator-commit-message-1.2.1.zip sharkfly666/ai-generator-commit-message v1.2.1 updatePlugins.xml

运行生成器测试：

    python3 -m unittest discover -s scripts -p 'test_*.py'

工作流需要 contents: write 权限和默认分支的推送权限。如果启用禁止机器人直接推送的分支保护规则，需要相应调整发布工作流。
