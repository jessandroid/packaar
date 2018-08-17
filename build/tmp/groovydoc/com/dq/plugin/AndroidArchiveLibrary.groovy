package com.dq.plugin

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.w3c.dom.Document
import org.w3c.dom.Element

import javax.xml.parsers.DocumentBuilderFactory

/****
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/10
 *
 **** */
class AndroidArchiveLibrary {

    Project mProject
    ResolvedArtifact mResolvedArtifact

    AndroidArchiveLibrary(Project project, ResolvedArtifact artifact) {
        if (!(PackAARPlugin.TYPE_AAR_ARTIFACT.equals(artifact.getType()))) {
            throw new IllegalArgumentException("artifact must be aar type !")
        }
        mProject = project
        mResolvedArtifact = artifact
    }

    File getExploadedRootDir() {
        println("BuildDir :" + mProject.getBuildDir().getPath())
        String path = mProject.getBuildDir().getPath() + File.separator + "intermediates" + File.separator + "exploded-aar" + File.separator
        println("path :" + path)
        File explodedRootDir = mProject.file(path)
        println("exploadedRootDir:" + explodedRootDir)
        ModuleVersionIdentifier id = mResolvedArtifact.getModuleVersion().getId()
        println("moduleVersionIdentifier :" + id)
        return mProject.file(explodedRootDir.getPath() + File.separator + id.getGroup() + File.separator + id.getName())
    }

    File getRootFolder() {
        File explodedRootDir = mProject.file(mProject.getBuildDir().getPath() + File.separator + "intermediates" + File.separator + "exploded-aar" + File.separator)
        ModuleVersionIdentifier id = mResolvedArtifact.getModuleVersion().getId()
        return mProject.file(explodedRootDir.getPath() + File.separator + id.getGroup() + File.separator + id.getName() + File.separator + id.getVersion())
    }

    List<File> getProguardRules() {
        List<File> list = new ArrayList<>()
        list.add(new File(getRootFolder(), "proguard-rules.pro"))
        list.add(new File(getRootFolder(), "proguard-project.txt"))
        return list
    }

    File getClassesJarFile() {
        return new File(getRootFolder(), "classes.jar")
    }

    File getManifest() {
        return new File(getRootFolder(), "AndroidManifest.xml")
    }

    String getPackageName() {
        String packageName = null
        File manifestFile = getManifest()
        if (manifestFile.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
                Document document = factory.newDocumentBuilder().parse(manifestFile)
                Element element = document.getDocumentElement()
                packageName = element.getAttribute("package")
            } catch (Exception e) {
                e.printStackTrace()
            }
        }
        return packageName
    }

    String getName() {
        return mResolvedArtifact.getModuleVersion().getId().getName()
    }

    String getVersion() {
        return mResolvedArtifact.getModuleVersion().getId().getVersion()
    }

    String getJniFolder(){
        return new File(getRootFolder(),"jni")
    }

    File getAssetsFolder() {
        return new File(getRootFolder(), "assets")
    }

    File getSymbol() {
        return new File(getRootFolder(), "R.txt")
    }

    File getResFolder() {
        return new File(getRootFolder(), "res")
    }

    Collection<File> getLocalJars() {
        List<File> localJars = new ArrayList<>()
        File[] jarList = new File(getRootFolder(), "libs").listFiles();
        if (jarList != null) {
            for (File jars : jarList) {
                if (jars.isFile() && jars.getName().endsWith(".jar")) {
                    localJars.add(jars)
                }
            }
        }
        return localJars
    }
}
