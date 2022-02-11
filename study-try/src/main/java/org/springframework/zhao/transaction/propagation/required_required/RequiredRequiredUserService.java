package org.springframework.zhao.transaction.propagation.required_required;

import org.springframework.zhao.transaction.demo02.User;

import java.util.List;

/**
 * 模拟业务类
 * @author : HeHaoZhao
 */
public interface RequiredRequiredUserService {

	void save();

	void save2();

}
