package com.manager;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.common.ErrorCode;
import com.datasource.*;
import com.exception.BusinessException;
import com.exception.ThrowUtils;
import com.model.Picture;
import com.model.dto.search.SearchRequest;
import com.model.dto.user.UserQueryRequest;
import com.model.enums.SearchTypeEnum;
import com.model.vo.PostVO;
import com.model.vo.SearchVO;
import com.model.vo.UserVO;
import com.service.PictureService;
import com.service.UserService;
import com.model.dto.post.PostQueryRequest;
import com.service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;

/**
 * 搜索门面
 * 门面模式
 */
@Component
@Slf4j
public class SearchFacade {
    @Resource
    private PostService postService;
    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;

    @Resource
    private PictureDataSource pictureDataSource;
    @Resource
    private PostDataSource postDataSource;
    @Resource
    private UserDataSource userDataSource;

    @Resource
    private DataSourceRegistry dataSourceRegistry;

    public SearchVO searchAll(@RequestBody SearchRequest searchRequest, HttpServletRequest request) {
        String type = searchRequest.getType();
        SearchTypeEnum searchTypeEnum = SearchTypeEnum.getEnumByValue(type);
        ThrowUtils.throwIf(StringUtils.isBlank(type), ErrorCode.PARAMS_ERROR);
        String searchText = searchRequest.getSearchText();
        long current = searchRequest.getCurrent();
        long pageSize = searchRequest.getPageSize();
        //搜索出所有数据
        if (searchTypeEnum == null) {
            //异步执行
            CompletableFuture<Page<Picture>> pictureTask = CompletableFuture.supplyAsync(() -> {
                Page<Picture> picturePage = pictureDataSource.doSearch(searchText, 1, 10);
                return picturePage;
            });

            CompletableFuture<Page<UserVO>> userTask = CompletableFuture.supplyAsync(() -> {
                UserQueryRequest userQueryRequest = new UserQueryRequest();
                userQueryRequest.setUserName(searchText);
                Page<UserVO> userVOPage = userDataSource.doSearch(searchText, current, pageSize);
                return userVOPage;
            });
            CompletableFuture<Page<PostVO>> postTask = CompletableFuture.supplyAsync(() -> {
                PostQueryRequest postQueryRequest = new PostQueryRequest();
                postQueryRequest.setSearchText(searchText);
                Page<PostVO> postVOPage = postDataSource.doSearch(searchText, current, pageSize);
                return postVOPage;
            });

            //相当于打了断点，会等所有线程执行完毕再执行下面的语句
            CompletableFuture.allOf(pictureTask, userTask, postTask).join();
            try {
                Page<Picture> picturePage = pictureTask.get();
                Page<UserVO> userVOPage = userTask.get();
                Page<PostVO> postVOPage = postTask.get();
                SearchVO searchVO = new SearchVO();
                searchVO.setUserList(userVOPage.getRecords());
                searchVO.setPostList(postVOPage.getRecords());
                searchVO.setPictureList(picturePage.getRecords());

                return searchVO;
            } catch (Exception e) {
                log.error("查询错误", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询异常");
            }
        } else {
            SearchVO searchVO = new SearchVO();

            /**
            switch (searchTypeEnum) {
                case POST:
                    PostQueryRequest postQueryRequest = new PostQueryRequest();
                    postQueryRequest.setSearchText(searchText);
                    Page<PostVO> postVOPage = postService.listPostVOByPage(postQueryRequest, request);
                    searchVO.setPostList(postVOPage.getRecords());
                    break;
                case USER:
                    UserQueryRequest userQueryRequest = new UserQueryRequest();
                    userQueryRequest.setUserName(searchText);
                    Page<UserVO> userVOPage = userService.listUserVOByPage(userQueryRequest);
                    searchVO.setUserList(userVOPage.getRecords());
                    break;
                case PICTURE:
                    Page<Picture> picturePage = pictureService.searchPicture(searchText, 1, 10);
                    searchVO.setPictureList(picturePage.getRecords());
                    break;
                default:
            }
            */

            //优化方式一
            /**
             *             DataSource dataSource = null;
             *             switch (searchTypeEnum) {
             *                 case POST:
             *                     dataSource = postDataSource;
             *                     break;
             *                 case USER:
             *                     dataSource = userDataSource;
             *                     break;
             *                 case PICTURE:
             *                     dataSource = pictureDataSource;
             *                     break;
             *                 case VIDEO:
             *                     break;
             *             }
             *             Page page = dataSource.doSearch(searchText, current, pageSize);
             */

            //优化方式二
            DataSource<?> dataSource = dataSourceRegistry.getDataSourceByType(type);
            Page<?> page = dataSource.doSearch(searchText, current, pageSize);
            searchVO.setDataList(page.getRecords());
            return searchVO;
        }
    }
}