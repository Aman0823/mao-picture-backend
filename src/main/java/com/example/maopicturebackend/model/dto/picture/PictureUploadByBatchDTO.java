package com.example.maopicturebackend.model.dto.picture;

import lombok.Data;

@Data
public class PictureUploadByBatchDTO {
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;
    /**
     * 名称前缀,管理员名字
     */
    private String namePrefix;
    /**
     * 图片名称
     */
    private String picName;


}
