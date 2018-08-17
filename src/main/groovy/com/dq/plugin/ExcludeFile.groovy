package com.dq.plugin
/****
 * 过滤的文件
 * @auth dengquan@360.cn
 * Create by dengquan on 2018/8/10
 *
 *****/
class ExcludeFile {
    /****
     * 文件所在根目录
     */
    private String name

    /*****
     * 文件名集合
     */
    private List<String> fileNames

    ExcludeFile(String name){
        this.name = name
    }

    def fileNames(String[] fileName) {
        this.fileNames = fileName.toList();
    }

    @Override
    String toString() {
        return "ExcludeFiles :"+rootFilename +"  fileNames:"+fileNames.toString()
    }
}
