package com.dream.xmlviewscanplugin


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * 扫描 Xml Plugin
 */
class XmlViewScanPlugin implements Plugin<Project>{

    void apply(Project project) {
        println 'Hello XmlViewScanPlugin'
        //添加黑名单扩展配置
        project.extensions.create('ignore',IgnoreViewExtension)

        project.afterEvaluate {
            //是否是 Android 插件
            def isAppPlugin = project.plugins.hasPlugin('com.android.application')

            //获取变体
            def variants
            if(isAppPlugin){
                variants = project.android.applicationVariants
            }else {
                variants = project.android.libraryVariants
            }

            variants.each{ variant ->
                //通过变体获取对应的 merge...Resources
                Task mergeResourcesTask = variant.mergeResources

                //定义自定义 Task 前缀
                def prefix = variant.name
                //获取我们自定义的 Task
                Task xmlViewScanTask = project.tasks.create("${prefix}XmlViewScanTask", XmlViewScanTask,variant)

                //将我们自定义的 Task 挂接到 mergeResourcesTask
                mergeResourcesTask.finalizedBy(xmlViewScanTask)
            }
        }
    }
}