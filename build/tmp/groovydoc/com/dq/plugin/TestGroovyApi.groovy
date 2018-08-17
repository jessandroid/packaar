package com.dq.plugin;

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies;

/****
 * 测试不了解的API
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/16
 *
 *****/
public class TestGroovyApi {
    private Project mProject
    public TestGroovyApi(Project project){
        mProject = project
    }

    /***
     * 获取API下的所有依赖
     */
    void testConfiguration(){
        Configuration configuration = mProject.configurations.findByName("api")
        mProject.gradle.addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies dependencies) {
                configuration.dependencies.each {
                    println("test Configuration ;"+it.name)
                }
            }

            @Override
            void afterResolve(ResolvableDependencies dependencies) {

            }
        })
       mProject.gradle.removeListener(this)
    }
}
