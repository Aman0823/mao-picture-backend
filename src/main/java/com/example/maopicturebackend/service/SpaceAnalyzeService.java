package com.example.maopicturebackend.service;

import com.example.maopicturebackend.model.dto.space.analyze.*;
import com.example.maopicturebackend.model.entity.Space;
import com.example.maopicturebackend.model.entity.User;
import com.example.maopicturebackend.model.vo.space.analyze.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SpaceAnalyzeService {
    SpaceUsageAnalyzeVO getSpaceUsageAnalyze(SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO,User user);

    List<SpaceCategoryAnalyzeVO> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO,User user);

    List<SpaceTagAnalyzeVO> getSpaceTagAnalyze(SpaceTagAnalyzeDTO spaceTagAnalyzeDTO,User user);

    List<SpaceSizeAnalyzeVO> getSpaceSizeAnalyze(SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO,User user);

    List<SpaceUserAnalyzeVO> getSpaceUserAnalyze(SpaceUserAnalyzeDTO spaceUserAnalyzeDTO,User user);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeDTO spaceRankAnalyzeDTO,User user);
}
