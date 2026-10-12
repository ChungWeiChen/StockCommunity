package com.stockcommunity.posts.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PostPageResponseDTO {

    private List<PostResponseDTO> posts;

    private int currentPage;

    private int totalPages;
}
