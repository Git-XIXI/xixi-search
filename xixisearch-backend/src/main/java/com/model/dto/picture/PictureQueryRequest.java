package com.model.dto.picture;

import com.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;


/**
 * 图片查询请求类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {

    //搜索词
    private String searchText;

    private static final long serialVersionUID = 1L;
}
