package com.dq.plugin

import joptsimple.internal.Strings
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.util.VersionNumber
import org.joor.Reflect

import java.lang.reflect.Field

/****
 * 获取插件的版本号
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/7
 *
 **** */
class AndroidGradlePluginHelper {

    static String getPluginVersionName() {
        try {
            String PLUGIN_VERSION_PROPERTIES = "com.android.builder.model.Version"
            String PLUGIN_VERSION_PARAM = "ANDROID_GRADLE_PLUGIN_VERSION"
            Class clz = Class.forName(PLUGIN_VERSION_PROPERTIES)
            Field field = clz.getDeclaredField(PLUGIN_VERSION_PARAM)
            field.setAccessible(true)
            return field.get(null)
        } catch (Exception e) {
            e.println()
            return -1
        }
    }


    static File resolveBundleDir(Project project, Object variant) {
        if (VersionNumber.parse(getPluginVersionName()).compareTo(VersionNumber.parse("2.3.0")) < 0) {
            String dirName = Reflect.on(variant).call("getDirName").get()
            if (Strings.isNullOrEmpty(dirName)) {
                return null
            }
            return project.file(project.getBuildDir() + "/intermediates/bundles/" + dirName)
        } else {
            Task mergeAssetsTask = Reflect.on(variant).call("getMergeAssets").get()
            File assetsDir = Reflect.on(mergeAssetsTask).call("getOutputDir").get()
            println("resolveBundleDir :"+assetsDir.getParentFile())
            return assetsDir.getParentFile()
        }
    }

    /****
     * return transform jar file
     * @param project
     * @param variant
     * @return
     */
    static File resolveTransform(Project project,Object variant){
        return project.file(project.getBuildDir() + "/intermediates/transforms/proguard/release/jars/3/3/main.jar")
    }

}
