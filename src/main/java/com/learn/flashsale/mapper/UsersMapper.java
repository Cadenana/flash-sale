package com.learn.flashsale.mapper;

import com.learn.flashsale.domain.po.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 哎嘿
 * @since 2024-07-02
 */
@Mapper
public interface UsersMapper extends BaseMapper<User> {

    List<String> getPermissionsById(Integer id);



    List<String> getRoleByIds(Integer id);
}
