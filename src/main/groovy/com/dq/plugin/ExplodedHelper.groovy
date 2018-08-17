package com.dq.plugin

import groovy.io.FileType
import joptsimple.internal.Strings
import org.gradle.api.Project;

/****
 * AAR 以及 Jar导出
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/10
 *
 **** */
class ExplodedHelper {

    static void processIntoJars(Project project, Collection<AndroidArchiveLibrary> androidArchiveLibraries, Collection<File> jarFiles, File folderOut, boolean minifyEnabled) {
        for (androidLibrary in androidArchiveLibraries){
            if(!androidLibrary.rootFolder.exists()){
                continue
            }
            def prefix = androidLibrary.name + "-" + androidLibrary.version
            if(!minifyEnabled){
                project.copy {
                    from(androidLibrary.classesJarFile)
                    into folderOut
                    rename {prefix + ".jar"}
                }
            }
            project.copy {
                from(androidLibrary.localJars)
                into folderOut
            }
        }
        for (jarFile in jarFiles){
            if(!jarFile.exists()){
                continue
            }
            project.copy {
                from(jarFile)
                into folderOut
            }
        }
    }

    /******
     *
     * @param project
     * @param androidArchiveLibraries
     * @param jarFiles
     * @param folderOut
     */
    static void processIntoClasses(Project project, Collection<AndroidArchiveLibrary> androidArchiveLibraries, Collection<File> jarFiles, File folderOut) {
        Collection<File> allJarFiles = new ArrayList<>()
        List<String> rPathList = new ArrayList<>()
        for (androidLibrary in androidArchiveLibraries) {
            println("androidLibrary rootFolder :" + androidLibrary.rootFolder)
            if (!androidLibrary.rootFolder.exists()) {
                println("PackAARPlugin-->[warning]" + androidLibrary.rootFolder + " not found!")
                continue
            }
            println('PackAARPlugin-->[androidLibrary]' + androidLibrary.getName())
            allJarFiles.add(androidLibrary.classesJarFile)
            String packageName = androidLibrary.getPackageName()
            if (!Strings.isNullOrEmpty(packageName)) {
                rPathList.add(packageName)
            }
        }
        for (jarFile in allJarFiles) {
            println("PackAARPlugin copy classes from :" + jarFile)
            project.copy {
                from project.zipTree(jarFile)
                into folderOut

                exclude 'META-INF/'
            }
        }
        println("replaceRClass start==============")
        replaceRImport(project, folderOut, rPathList)
        println("replaceRClass end================")

        println("cleanRClass start=============")
        cleanRFile(folderOut, rPathList)
        println("cleanRClass end================")

    }

    /****
     * 替换输出目录下的所有引用到R文件的class
     * @param project
     * @param folderOut
     * @param rPathList
     */
    static void replaceRImport(Project project, File folderOut, List<String> rPathList) {
        def rBytes
        def rFilePath
        def packageName = getProjectPackage(project)
        rPathList.each { rPath ->
            rBytes = (rPath.replace('.', '/') + 'R').getBytes().toString()
            rBytes = rBytes.substring(1, rBytes.length() - 1)
            rFilePath = folderOut.absolutePath + '\\' + rPath.replace('.', '\\')
            File baseFolder = new File(rFilePath)
            if (baseFolder.exists()) {
                baseFolder.traverse(
                        type: FileType.FILES
                ) { file ->
                    if (file.getBytes().toString().contains(rBytes)) {
                        if (!file.name.contains("R.class") && !file.name.startsWith('R$')) {
                            replaceRClass(file, (rPath.replace('.', '/') + '/R'), (packageName.replace('.', '/') + '/R'))
                        }
                    }
                }
            }
        }
    }

    static void cleanRFile(File folderFile, List<String> rPathList) {
        def rFilePath
        rPathList.each { rPath ->
            rFilePath = folderFile.absolutePath + '\\' + rPath.replace('.', '\\')
            println("clean File :" + rFilePath)
            File baseFolder = new File(rFilePath)
            if (baseFolder.exists()) {
                baseFolder.traverse(
                        type: FileType.FILES,
                        nameFilter: ~/((^R)|(^R\$.*))\.class/
                ) { file ->
                    println("delete R File :" + file.absolutePath + "  " + file.delete())
                }
            }
        }
    }

    /****
     *
     * @param project
     * @return
     */
    static String getProjectPackage(Project project) {
        def manifestFile = project.projectDir.absolutePath + "/src/main/AndroidManifest.xml"
        def xparser = new XmlParser()
        def androidManifest = xparser.parse(manifestFile)
        return androidManifest.@'manifest:package'
    }

}
