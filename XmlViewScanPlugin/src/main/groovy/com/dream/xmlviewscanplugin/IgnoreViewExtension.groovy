package com.dream.xmlviewscanplugin


/**
 * 配置黑名单：一些 View 不需要被扫描
 *
 * 主要针对一些特殊的 View ：构造方法是私有的，不能被 new 出来
 */
class IgnoreViewExtension{


    /**是否开启黑名单功能 默认开启*/
    boolean isEnable = true

    /**黑名单 View 集合*/
    List<String> ignoreViewList
}