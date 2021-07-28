package com.dream.xmlviewscanplugin

import com.android.build.gradle.api.BaseVariant
import groovy.util.slurpersupport.GPathResult
import groovy.util.slurpersupport.Node
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.stream.Stream

/**
 * 扫描 Xml Task
 */
class XmlViewScanTask extends DefaultTask {

    /**
     * Xml 布局中被添加进来的 View
     */
    private Set<String> mXmlScanViewSet = new HashSet<>()

    private BaseVariant variant

    @Inject
    XmlViewScanTask(BaseVariant variant) {
        this.variant = variant
    }


    /**
     * 执行 xml 扫描 Task
     */
    @TaskAction
    void performXmlScanTask() {
        try {
            println 'performXmlScanTask start...'

            //创建需要输出 View 的文件路径
            File outputFile = new File(project.buildDir.path + "/${variant.name}_xml_scan_view/xml_scan_view.txt")
            if (!outputFile.parentFile.exists()) {
                outputFile.parentFile.mkdirs()
            }
            if (outputFile.exists()) {
                outputFile.delete()
            }
            outputFile.createNewFile()
            println 'file create success...'
            mXmlScanViewSet.clear()

            //获取 merger.xml 文件
            Task mergeResourcesTask = variant.mergeResources
            String mergerPath = "${project.buildDir.path}/intermediates/incremental/${mergeResourcesTask.name}/merger.xml"
            File mergerFile = new File(mergerPath)

            //开始解析  merger.xml
            XmlSlurper xmlSlurper = new XmlSlurper()
            GPathResult result = xmlSlurper.parse(mergerFile)
            if (result.children()) {
                result.childNodes().forEachRemaining(new Consumer() {
                    @Override
                    void accept(Object o) {
                        parseNode(o)
                    }
                })
            }
            println 'merger.xml parsing success...'


            //到这里，所有的 xml 控件都被添加到了mXmScanViewSet
            //接下来我们就需要读取黑名单中的 View 并给过滤掉
            Stream<String> viewNameStream
            //是否开启黑名单过滤功能
            if(project.ignore.isEnable){
                println 'blacklist enable...'
                viewNameStream = filterXmlScanViewSet()

                //如果此时没有配置黑名单 viewNameStream 还是会为 null
                if(viewNameStream == null){
                    viewNameStream = mXmlScanViewSet.stream()
                }
            }else {
                println 'blacklist disable...'
                viewNameStream = mXmlScanViewSet.stream()
            }

            //将 viewName 写入文件中
            PrintWriter printWriter = new PrintWriter(new FileWriter(outputFile))
            viewNameStream.forEach(new Consumer<String>() {
                @Override
                void accept(String viewName) {
                    printWriter.println(viewName)
                }
            })
            printWriter.flush()
            printWriter.close()
            println 'write all viewName to file success...'
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * 过滤黑名单中的 viewName
     * @return Stream<String>
     */
    private Stream<String> filterXmlScanViewSet() {
        List<String> ignoreViewList = project.ignore.ignoreViewList
        Stream<String> viewNameStream = null
        if (ignoreViewList) {
            println "ignoreViewList: $ignoreViewList"
            viewNameStream = mXmlScanViewSet.stream().filter(new Predicate<String>() {
                @Override
                boolean test(String viewName) {
                    for (String ignoreViewName : ignoreViewList) {
                        if (viewName == ignoreViewName) {
                            return false
                        }
                    }
                    return true
                }
            })
        }else {
            println 'ignoreViewList is null, no filter...'
        }
        return viewNameStream
    }



    /**
     * 递归解析 merger.xml 中的 Node 节点
     *
     * merger.xml 文件中的 布局文件标签如下：
     * <file name="activity_main"
     *       path="/Users/zhouying/learning/GradleDemo/app/src/main/res/layout/activity_main.xml"
     *       qualifiers=""
     *       type="layout"/>
     */
    private void parseNode(Object obj) {
        if (obj instanceof Node) {
            Node node = obj

            if (node) {
                if ("file" == node.name() && "layout" == node.attributes().get("type")) {
                    //获取布局文件
                    String layoutPath = node.attributes().get("path")
                    File layoutFile = new File(layoutPath)

                    //开始解析布局文件
                    XmlSlurper xmlSlurper = new XmlSlurper()
                    GPathResult result = xmlSlurper.parse(layoutFile)
                    String viewName = result.name()
                    mXmlScanViewSet.add(viewName)


                    if (result.children()) {
                        result.childNodes().forEachRemaining(new Consumer() {
                            @Override
                            void accept(Object o) {
                                //递归解析子节点
                                parseLayoutNode(o)
                            }
                        })
                    }
                } else {
                    //如果不是布局文件，递归调用
                    node.childNodes().forEachRemaining(new Consumer() {
                        @Override
                        void accept(Object o) {
                            parseNode(o)
                        }
                    })

                }
            }
        }
    }


    /**
     * 递归解析 layout 布局子节点
     */
    private void parseLayoutNode(Object obj) {
        if (obj instanceof Node) {
            Node node = obj
            if (node) {
                mXmlScanViewSet.add(node.name())
                node.childNodes().findAll {
                    parseLayoutNode(it)
                }
            }
        }
    }

}