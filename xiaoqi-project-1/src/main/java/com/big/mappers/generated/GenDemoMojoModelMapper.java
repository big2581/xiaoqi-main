package com.big.mappers.generated;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import com.big.models.DemoMojoModel;

@Mapper
public interface GenDemoMojoModelMapper
{
	
	List<DemoMojoModel> find(Map<String, Object> params);
	Long getTotal(Map<String, Object> params);
}
