package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.service.IFollowService;
import org.springframework.web.bind.annotation.*;
import org.yaml.snakeyaml.events.Event;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/follow")
public class FollowController {
    @Resource
    private IFollowService iFollowService;

    @PutMapping("/{id}/{isFollow}")
    public Result follow(@PathVariable("id") Long followUserId , @PathVariable Boolean isFollow){
        return iFollowService.follow(followUserId,isFollow);
    }

    @GetMapping("/or/not/{id}")
    public Result isFollow(@PathVariable Long id){
        return iFollowService.isFollow(id);
    }

    @GetMapping("/common/{id}")
    public Result common(@PathVariable Long id){
        return iFollowService.common(id);
    }

}
