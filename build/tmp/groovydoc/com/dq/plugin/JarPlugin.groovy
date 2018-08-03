package com.dq.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project;

/****
 * 打包Jar的插件
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/3
 *
 *****/
public class JarPlugin implements Plugin<Project>{
    @Override
    void apply(Project target) {
        println("welcome to JarPlugin world !!!!!!")
    }
}
