/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.converter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A converter converts a source object of type {@code S} to a target of type {@code T}.
 *
 * <p>Implementations of this interface are thread-safe and can be shared.
 *
 * <p>Implementations may additionally implement {@link ConditionalConverter}.
 *
 * @author Keith Donald
 * @author Josh Cummings
 * @since 3.0
 * @param <S> the source type
 * @param <T> the target type
 */
@FunctionalInterface
public interface Converter<S, T> {

	/**
	 * Convert the source object of type {@code S} to target type {@code T}.
	 * 将S类型的源对象转换为T类型的目标对象。
	 *
	 * @param source the source object to convert, which must be an instance of {@code S} (never {@code null})      要转换的源对象，必须是S的实例（从不为null）
	 * @return the converted object, which must be an instance of {@code T} (potentially {@code null})      转换的对象，必须是T的实例（可能为null）
	 * @throws IllegalArgumentException if the source cannot be converted to the desired target type
	 */
	@Nullable
	T convert(S source);

	/**
	 * Construct a composed {@link Converter} that first applies this {@link Converter} to its input, and then applies the {@code after} {@link Converter} to the result.
	 * 构造一个组合转换器，首先将此转换器应用于其输入，然后将后转换器应用于结果。
	 *
	 * @param after the {@link Converter} to apply after this {@link Converter} is applied      应用此转换器后要应用的转换器
	 * @param <U> the type of output of both the {@code after} {@link Converter} and the composed {@link Converter}     后转换器和合成转换器的输出类型
	 * @return a composed {@link Converter} that first applies this {@link Converter} and then applies the {@code after} {@link Converter}
	 *          先应用此转换器，然后应用后转换器的组合转换器
	 * @since 5.3
	 */
	default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
		Assert.notNull(after, "After Converter must not be null");
		return (S s) -> {
			T initialResult = convert(s);
			return (initialResult != null ? after.convert(initialResult) : null);
		};
	}

}
