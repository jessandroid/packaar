package com.dq.plugin

import com.android.build.gradle.api.AndroidBasePlugin
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.tasks.InvokeManifestMerger
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.internal.artifacts.DefaultResolvedArtifact
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar

/****
 * 自定义处理任务
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/10
 *
 **** */
class VariantProcessor {
    /****
     * project
     */
    Project mProject
    /****
     * 变种
     */
    LibraryVariant mLibraryVariant

    /*****
     * 项目所有的依赖
     */
    Set<ResolvedArtifact> mResolveArtifacts = new ArrayList<>()

    /***
     * 过滤的文件集合
     */
    List<ExcludeFile> mExcludeFiles = new ArrayList<>()

    Collection<File> mJarFiles = new ArrayList<>()

    Collection<AndroidArchiveLibrary> mAndroidArchiveLibraries = new ArrayList<>()

    VariantProcessor(Project project, LibraryVariant libraryVariant) {
        mProject = project
        mLibraryVariant = libraryVariant
        println("projectName :" + mProject.getName() + "  variantNam : " + mLibraryVariant.getName())
    }

    void addArtifacts(Set<ResolvedArtifact> set) {
        mResolveArtifacts.addAll(set)
    }

    void addExcludedFiles(List<ExcludeFile> list) {
        mExcludeFiles.addAll(list)
    }

    void processVariant() {
        println("start process variant")
        String taskPath = "pre" + mLibraryVariant.name.capitalize() + "Build"
        Task prepareTask = mProject.tasks.findByPath(taskPath)
        if (prepareTask == null) {
            throw new RuntimeException("Can not find task ${taskPath} !!!!")
        }
        taskPath = "bundle" + mLibraryVariant.name.capitalize()
        Task bundleTask = mProject.tasks.findByPath(taskPath)
        if (bundleTask == null) {
            throw new RuntimeException("Can not find task ${taskPath} !!!")
        }
        processArtifacts(bundleTask)

        processClassesAndJars()

        if (mAndroidArchiveLibraries.isEmpty()) {
            return
        }

//        processManifest()

        processResourcesAndR()

        processRSources()

        processAssets()

        processJniLibs()

        processProguardTxt(prepareTask)

        mergeRClass(bundleTask)
        //Execution failed for task ':netlib:generateReleaseRFile'.java.util.NoSuchElementException (no error message)

        processExcludeFiles()
    }

    /****
     * exploded artifact files
     * @param bundleTask
     */
    void processArtifacts(Task bundleTask) {
        for (DefaultResolvedArtifact artifact in mResolveArtifacts) {
            println("processArtifacts :" + artifact.getName())
            if (PackAARPlugin.TYPE_JAR_ARTIFACT.equals(artifact.type)) {
                addJarFile(artifact.file)
            }
            if (PackAARPlugin.TYPE_AAR_ARTIFACT.equals(artifact.type)) {
                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(mProject, artifact)
                addAndroidArchiveLibrary(archiveLibrary)
                Set<Task> buildDependencies = artifact.getBuildDependencies().getDependencies()
                buildDependencies.each { println("buildDependency Name :" + it.getName()) }
                println("exploadedRootDir :" + archiveLibrary.getExploadedRootDir())
                archiveLibrary.getExploadedRootDir().deleteDir()
                def zipFolder = archiveLibrary.getRootFolder()
                println("zipFolder :" + zipFolder)
                zipFolder.mkdirs()
                if (buildDependencies.size() == 0) {
                    mProject.copy {
                        println("Copy Project File Origin Path :" + artifact.file.absolutePath)
                        from mProject.zipTree(artifact.file.absolutePath)
                        into zipFolder
                    }
                } else {
                    Task explodTask = mProject.tasks.create(name: "explod" + artifact.name.capitalize() + mLibraryVariant.buildType.name, type: Copy) {
                        println("Copy Project File Origin Path :" + artifact.file.absolutePath)
                        from mProject.zipTree(artifact.file.absolutePath)
                        into zipFolder
                    }
                    println("explodTask :" + explodTask.getName() + "  buildFirstTask :" + buildDependencies.first().getName() + "   bundleTask :" + bundleTask.getName())
                    explodTask.dependsOn(buildDependencies.first())
                    explodTask.shouldRunAfter(buildDependencies.first())
                    bundleTask.dependsOn(explodTask)
                }
            }
        }
    }

    /*****
     * 合并所有的类文件,拷贝到packagedAssets/libs目录下面
     */
    private void processClassesAndJars() {
        if (mLibraryVariant.getBuildType().isMinifyEnabled()) {
            println("minifyEnabled :true")
            for (archiveLibrary in mAndroidArchiveLibraries) {
                List<File> thirdProguardFiles = archiveLibrary.getProguardRules()
                for (File file : thirdProguardFiles) {
                    if (file.exists()) {
                        println("proguard file path :" + file.absolutePath)
                        mProject.android.getDefaultConfig().proguardFile(file)
                    }
                }
            }
            Task javacTask = mLibraryVariant.getJavaCompile()
            if (javacTask == null) {
                return
            }
            println("javacTask Name :" + javacTask.getName())
            javacTask.doLast {
                def dustDir = mProject.file(mProject.buildDir.path + File.separator + "intermediates" + File.separator + "classes" + File.separator + mLibraryVariant.dirName)
                println("dustDir path :" + dustDir)
                ExplodedHelper.processIntoClasses(mProject, mAndroidArchiveLibraries, mJarFiles, dustDir)
            }
        }
        String taskPath = "transformClassesAndResourcesWithSyncLibJarsFor" + mLibraryVariant.name.capitalize()
        Task syncLibTask = mProject.tasks.findByPath(taskPath)
        if (syncLibTask == null) {
            throw new RuntimeException("Can no find task ${taskPath}")
        }
        syncLibTask.doLast {
            def dustDir = mProject.file(AndroidGradlePluginHelper.resolveBundleDir(mProject, mLibraryVariant).path + "/libs")
            ExplodedHelper.processIntoJars(mProject, mAndroidArchiveLibraries, mJarFiles, dustDir, mLibraryVariant.getBuildType().isMinifyEnabled())
        }
    }

    /****
     * 合并Manifest文件
     */
    private void processManifest() {
        Class invokeManifestTaskClazz = null
        String className = "com.android.build.gradle.tasks.InvokeManifestMerger"
        try {
            invokeManifestTaskClazz = Class.forName(className)
        } catch (ClassNotFoundException e) {
            e.printStackTrace()
        }
        if (invokeManifestTaskClazz == null) {
            throw new RuntimeException("Can not find class ${className} !!!!")
        }
        Task processManifestTask = mLibraryVariant.getOutputs().first().getProcessManifest()
        println("processManifestName :" + processManifestTask.getName())
        def manifestOutput = mProject.file(mProject.buildDir.path + "/intermediates/fat-aar/" + mLibraryVariant.dirName)
        println("manifestOutput :" + manifestOutput)
        File manifestOutputBackup = mProject.file(processManifestTask.getManifestOutputDirectory().absolutePath + "/AndroidManifest.xml")
        processManifestTask.setManifestOutputDirectory(manifestOutput)
        File mainManifestFile = new File(manifestOutput.absolutePath + "/AndroidManifest.xml")
        mainManifestFile.deleteOnExit()
        manifestOutput.mkdirs()
        mainManifestFile.createNewFile()
        processManifestTask.doLast {
            mainManifestFile.write(manifestOutputBackup.text)
        }
        InvokeManifestMerger manifestMergerTask = mProject.tasks.create('merge' + mLibraryVariant.name.capitalize() + "Manifest", invokeManifestTaskClazz)
        manifestMergerTask.setVariantName(mLibraryVariant.name)
        manifestMergerTask.setMainManifestFile(mainManifestFile)
        List<File> list = new ArrayList<>()
        for (archiveLibrary in mAndroidArchiveLibraries) {
            list.add(archiveLibrary.getManifest())
        }
        manifestMergerTask.setSecondaryManifestFiles(list)
        manifestMergerTask.setOutputFile(manifestOutputBackup)
        manifestMergerTask.dependsOn processManifestTask
        manifestMergerTask.doFirst {
            List<File> existFiles = new ArrayList<>()
            manifestMergerTask.getSecondaryManifestFiles().each {
                if (it.exists()) {
                    existFiles.add(it)
                }
            }
            manifestMergerTask.setSecondaryManifestFiles(existFiles)
        }
        processManifestTask.finalizedBy manifestMergerTask
    }


    private void processResourcesAndR() {
        String taskPath = "generate" + mLibraryVariant.name.capitalize() + "Resources"
        Task resourceGenTask = mProject.tasks.findByPath(taskPath)
        println("resourceGenTask Name :" + resourceGenTask.getName())
        if (resourceGenTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}")
        }
        resourceGenTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                mProject.android.sourceSets."main".res.srcDir(archiveLibrary.resFolder)
            }
        }
    }

    /***
     * generate R.java
     */
    private void processRSources() {
        try {
            Task processResourcesTask = mLibraryVariant.getOutputs().first().getProcessResources()
            for (BaseVariantOutput output : mLibraryVariant.getOutputs()) {
                println("outName :" + output.getName() + " DirName:" + output.getDirName() + "   taskName :" + output.getProcessResources())
            }
            println("start process ResoucesTask" + processResourcesTask.getName() + "  size :" + mLibraryVariant.getOutputs().size()
                    + "  name:" + mLibraryVariant.getOutputs().first().toString() + "  taskName:" + mLibraryVariant.getOutputs().first().getProcessResources().toString())
            processResourcesTask.doLast {
                println("mAndroidArchiveLibraries size :" + mAndroidArchiveLibraries.size())
                for (archiveLibrary in mAndroidArchiveLibraries) {
                    println("generate R.java sourceOutputDir:" + processResourcesTask.getSourceOutputDir())
                    RSourcesGenerate.generate(processResourcesTask.getSourceOutputDir(), archiveLibrary)
                }
            }
            println("end process ResourcesTask")
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    void processAssets() {
        Task assetsTask = mLibraryVariant.getMergeAssets()
        if (assetsTask == null) {
            throw new RuntimeException("Can not find task in variant.getMergeAssets")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            assetsTask.getInputs().dir(archiveLibrary.assetsFolder)
        }
        assetsTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                mProject.android.sourceSets."main".assets.srcDir(archiveLibrary.assetsFolder)
            }
        }
    }

    private void processJniLibs() {
        String taskPath = "merge" + mLibraryVariant.name.capitalize() + "JniLibFolders"
        Task mergeJniLibsTask = mProject.tasks.findByPath(taskPath)
        if (mergeJniLibsTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            println("jniFolder :" + archiveLibrary.jniFolder)
            mergeJniLibsTask.getInputs().dir(archiveLibrary.jniFolder)
        }
        mergeJniLibsTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                mProject.android.sourceSets."main".jniLibs.srcDir(archiveLibrary.jniFolder)
            }
        }
    }

    void processProguardTxt(Task prepareTask) {
        String taskPath = "merge" + mLibraryVariant.name.capitalize() + "ConsumerProguardFiles"
        Task mergeFileTask = mProject.tasks.findByPath(taskPath)
        println("processProguardTxt Name :" + taskPath + " taskName :" + mergeFileTask.getName())
        if (mergeFileTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            List<File> thirdProguardFiles = archiveLibrary.proguardRules
            for (File file : thirdProguardFiles) {
                if (file.exists()) {
                    println("add proguard file :" + file.path)
                    mergeFileTask.getInputs().file(file)
                }
            }
        }
        mergeFileTask.doFirst {
            Collection proguardFiles = mergeFileTask.getInputFiles()
            for (archiveLibrary in mAndroidArchiveLibraries) {
                List<File> thirdProguardFiles = archiveLibrary.proguardRules
                for (File file : thirdProguardFiles) {
                    if (file.exists()) {
                        println("add proguard file :" + file.path)
                        proguardFiles.add(file)
                    }
                }
            }
        }
        mergeFileTask.dependsOn prepareTask
    }

    /***
     * merge android library R.class
     * @param bundleTask
     */
    void mergeRClass(Task bundleTask) {
        String taskPath = "transformClassesAndResourcesWithSyncLibJarsFor" + mLibraryVariant.name.capitalize()
        Task syncLibTask = mProject.tasks.findByPath(taskPath)
        if (syncLibTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}")
        }
        def classesJar = mProject.file(AndroidGradlePluginHelper.resolveBundleDir(mProject, mLibraryVariant).path + "/classes.jar")
        String applicationId = mLibraryVariant.getApplicationId()
        String excludeRPath = applicationId.replace('.', '/')
        Task jarTask
        //开启混淆时对混淆过后的文件进行重新打包
        if (mLibraryVariant.getBuildType().isMinifyEnabled()) {
            jarTask = mProject.tasks.create(name: "transformProguardJarTask" + mLibraryVariant.name, type: Jar) {
                from project.zipTree(AndroidGradlePluginHelper.resolveTransform(mProject, mLibraryVariant))
                exclude(excludeRPath + "/R.class", excludeRPath + '/R$*', "META-INF/")
            }
            jarTask.onlyIf {
                File file = AndroidGradlePluginHelper.resolveTransform(mProject, mLibraryVariant)
                return file.exists()
            }
            jarTask.doLast {
                File file = new File(mProject.getBuildDir().absolutePath + '/libs/' + mProject.name + ".jar")
                if (file.exists()) {
                    mProject.delete(classesJar)
                    mProject.copy {
                        from(file)
                        into(AndroidGradlePluginHelper.resolveBundleDir(mProject, mLibraryVariant))
                        rename(mProject.name + ".jar", "classes.jar")
                    }
                } else {
                    println 'can not find transformProguradJar file '
                }
            }
        } else {
            jarTask = mProject.tasks.create(name: "transformJarTask" + mLibraryVariant.name, type: Jar) {
                from(mProject.buildDir.absolutePath + "/intermediates/classes/" + mLibraryVariant.name.capitalize())
                exclude(excludeRPath + "/R.class", excludeRPath + '/R$*', "META-INF/")
            }
            jarTask.onlyIf {
                File file = mProject.file(mProject.buildDir.absolutePath + "/intermediates/classes/" + mLibraryVariant.name.capitalize())
                return file.exists()
            }
            jarTask.doLast {
                println("transform jar ready ")
                File file = new File(mProject.getBuildDir().absolutePath + "/libs/" + mProject.name + ".jar")
                if (file.exists()) {
                    mProject.delete(classesJar)
                    mProject.copy {
                        from(file)
                        into(AndroidGradlePluginHelper.resolveBundleDir(mProject, mLibraryVariant))
                        rename(mProject.name + ".jar", "classes.jar")
                    }
                } else {
                    println("can not find transformProguardjar file")
                }
            }
        }
        bundleTask.dependsOn jarTask
        jarTask.shouldRunAfter(syncLibTask)
    }

    /***
     * delete exclude files
     */
    void processExcludeFiles() {
        String taskPath = "bundle" + mLibraryVariant.name.capitalize()
        Task bundleTask = mProject.tasks.findByPath(taskPath)
        if (bundleTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}")
        }
        taskPath = 'transformClassesAndResourcesWithSyncLibJarsFor' + mLibraryVariant.name.capitalize()
        Task syncLibTask = mProject.tasks.findByPath(taskPath)
        if (syncLibTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}")
        }
        def excludeFileTask = mProject.tasks.create(name: 'transformExcludeFilesTask' + mLibraryVariant.name)
        excludeFileTask.doLast {
            def bundlePath = AndroidGradlePluginHelper.resolveBundleDir(mProject, mLibraryVariant).path
            mExcludeFiles.each { excludeFile ->
                excludeFile.fileNames.each { fileName ->
                    File file = mProject.file(bundlePath + File.separator + excludeFile.name + File.separator + fileName)
                    println(file.path)
                    if(file.exists()){
                        file.delete()
                    }else {
                        println 'excludeFileError : ' + file.path + ' not exist'
                    }
                }
            }
        }
        bundleTask.dependsOn excludeFileTask
        excludeFileTask.shouldRunAfter(syncLibTask)
    }

    void addAndroidArchiveLibrary(AndroidArchiveLibrary library) {
        mAndroidArchiveLibraries.add(library)
    }

    void addJarFile(File jar) {
        mJarFiles.add(jar)
    }
}
