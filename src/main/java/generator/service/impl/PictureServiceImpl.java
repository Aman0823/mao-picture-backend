package generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import generator.domain.Picture;
import generator.service.PictureService;
import generator.mapper.PictureMapper;
import org.springframework.stereotype.Service;

/**
* @author mao
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2025-02-14 20:27:59
*/
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

}




