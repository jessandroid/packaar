package com.dq.plugin

import org.gradle.api.NamedDomainObjectCollection

/****
 * 自定义Extension，插件库的配置项
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/7
 *
 *****/
class PackAARExtension {

    /*****
     * 是否开启插件
     */
    boolean enabled

    NamedDomainObjectCollection<ExcludeFile> excludeFiles

    PackAARExtension(NamedDomainObjectCollection<ExcludeFile> excludeFiles){
        this.excludeFiles = excludeFiles
    }

    def excludeFiles(Closure closure) {
        excludeFiles.configure(closure)
    }

    def enabled(boolean enabled) {
        this.enabled = enabled
    }

    @Override
    String toString() {
        return  "PackAARPlugin{" +
                "enable=" + enable
    }
}
