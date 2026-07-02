package com.itmal.common.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikeResponseDto {

    private boolean liked;
    private int likeCount;

    public LikeResponseDto(boolean liked, int likeCount){
        this.liked = liked;
        this.likeCount = likeCount;
    }


}
