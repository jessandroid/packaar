package com.dq.plugin

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.LibraryVariant
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.internal.component.model.Exclude;

/****
 * 打包Jar的插件
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/3
 *
 **** */
class PackAARPlugin implements Plugin<Project> {

    static final String TYPE_AAR_ARTIFACT = "aar"
    static final String TYPE_JAR_ARTIFACT = "jar"

    static final String PLUGIN_ANDROID_LIBRARY_NAME = "com.android.library"
    static final String PLUGIN_EXTENSION_NAME = "packAARExt"
    Project mProject

    PackAARExtension mPackAARExtension
    Configuration mConfiguration
    /***
     * 依赖的module
     */
    Set<ResolvedArtifact> mArtifacts

    @Override
    void apply(Project target) {
        println("welcome to PackAARPlugin world !!!!!!")

        mProject = target
        println("Current Project Name :" + mProject.getDisplayName())

        initPlugin()
    }

    void initPlugin() {
        //打印Gradle plugin的版本号信息
        String pluginVersionName = AndroidGradlePluginHelper.getPluginVersionName()
        println("Gradle Plugin VerisonName :" + pluginVersionName)

        //添加Extension
        mProject.extensions.add(PLUGIN_EXTENSION_NAME, new PackAARExtension(mProject.container(ExcludeFile)))

        //检查项目是否包含library module
        checkAndroidLibrary()

        //修改项目（library）配置依赖
        createLibraryConfiguration()

        //初始化配置信息完毕
        afterEvaluated()
    }

    /****
     * 没有包含com.android.library直接抛出异常信息
     */
    void checkAndroidLibrary() {
        if (mProject == null) {
            throw new NullPointerException("PackAARPlugin init error ,please try again !!!!!")
        }
        if (mProject != null && !mProject.plugins.hasPlugin(PLUGIN_ANDROID_LIBRARY_NAME)) {
            throw new ProjectConfigurationException('PackAARPlugin must be applied in project that' +
                    ' has android library plugin!', null)
        }
    }

    /****
     *  修改项目配置依赖
     * 当开启fat-aar插件时 将所有embed修饰的依赖库全部改为 私有依赖
     * 当关闭fat-aar插件时 将所有embed修饰的依赖库全部改为 普通依赖
     */
    void createLibraryConfiguration() {
        mProject.configurations.each {
//            println("configurationName :" + it.toString())
        }
        mConfiguration = mProject.configurations.create("embed")
        mConfiguration.visible = false
//        testApi()
        mProject.gradle.addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies dependencies) {
                println("beforeResolve :" + dependencies.toString())
                mPackAARExtension = mProject.packAARExt
                if (mPackAARExtension != null && mPackAARExtension.enabled) {
                    println("enabled PackAARPlugin And change embed to compileOnly")
                    mConfiguration.dependencies.each { dependency ->
                        mProject.dependencies.add("compileOnly", dependency)
                        println("dependencyName :" + dependency.getName())
                    }
                } else {
                    println("disabled PackAARPlugin And change embed to api")
                    mConfiguration.dependencies.each { dependency ->
                        mProject.dependencies.add("api", dependency)
                    }
                }
                mProject.gradle.removeListener(this)
            }

            @Override
            void afterResolve(ResolvableDependencies dependencies) {

            }
        })
    }

    /****
     * 结束完毕配置项
     */
    void afterEvaluated() {
        mProject.afterEvaluate {
            mPackAARExtension = mProject.packAARExt
            if (mPackAARExtension != null && mPackAARExtension.enabled) {
                println("after evaluated")
                resolveArtifacts()
                LibraryExtension android = mProject.android
                android.libraryVariants.all {
                    resolveVariantProcess(it, mPackAARExtension.excludeFiles)
                }
            }
        }
    }

    /****
     * 解析项目所有依赖库
     */
    void resolveArtifacts() {
        def set = new HashSet()
        mConfiguration.resolvedConfiguration.resolvedArtifacts.each { artifact ->
            println("resolve artifacts :" + artifact.getName() + "  type :" + artifact.type)
            if (TYPE_AAR_ARTIFACT.equals(artifact.type) || TYPE_JAR_ARTIFACT.equals(artifact.type)) {
                println 'pack-aar-->[embed detected][' + artifact.type + ']' + artifact.moduleVersion.id
            } else {
                throw new ProjectConfigurationException("Only support embed aar and jar dependencies!")
            }
            set.add(artifact)
            mArtifacts = Collections.unmodifiableSet(set)
        }
    }

    /***
     * 自定义打包处理任务
     */
    void resolveVariantProcess(LibraryVariant libraryVariant, NamedDomainObjectCollection<ExcludeFile> excludeFileNames) {
        println("start resolve variant process")
        VariantProcessor processor = new VariantProcessor(mProject,libraryVariant)
        processor.addArtifacts(mArtifacts)
        processor.addExcludedFiles(excludeFileNames.asList())
        processor.processVariant()
        println("end resolve variant process")
    }

    /****
     * 测试不了解的API
     */
    void testApi(){
        TestGroovyApi api = new TestGroovyApi(mProject)
        api.testConfiguration()
    }
}
