package com.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.model.Picture;

public interface PictureService {

    public Page<Picture> searchPicture(String searchText, long pageNum, long pageSize);
}
