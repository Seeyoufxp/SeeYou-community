package com.seeyou.ai.function;

import com.seeyou.ai.client.UserClient;
import com.seeyou.ai.client.dto.RegisterInfoDTO;
import com.seeyou.ai.pojo.dto.WeatherInfo;
import com.seeyou.ai.service.QWeatherService;
import com.seeyou.common.context.UserContext;
import com.seeyou.common.result.R;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * AI Function Calling 函数注册
 * Spring AI 通过 @Bean Function<I,O> + @Description 注册可被 LLM 调用的函数：
 *   - getUserRegisterInfo：获取当前登录用户的注册时长/城市（供欢迎语生成）
 *   - getCurrentWeather：获取当前登录用户所在城市的实时天气（供欢迎语生成）
 *
 * 两个 Function 均无需 LLM 传参（当前用户身份从 UserContext 取），LLM 只需决定是否调用。
 * ChatClient 调用时通过 .functions("getUserRegisterInfo","getCurrentWeather") 绑定。
 * Function 在 ChatClient 调用线程同步执行，UserContext 可用。
 *
 * 任意远程调用失败均降级返回 null，LLM 会基于已有信息生成欢迎语，不阻断主流程。
 */
@Configuration
public class FunctionConfig {

    @Bean
    @Description("获取当前登录用户的注册信息，包括昵称、注册天数、所在城市。无需传参。")
    public Function<FunctionEmptyRequest, RegisterInfoResponse> getUserRegisterInfo(UserClient userClient) {
        return req -> {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return null;
            }
            try {
                R<RegisterInfoDTO> resp = userClient.getRegisterInfo(userId);
                if (resp == null || resp.getData() == null) {
                    return null;
                }
                RegisterInfoDTO info = resp.getData();
                int days = info.getCreateTime() == null ? 0
                        : (int) (LocalDate.now().toEpochDay() - info.getCreateTime().toLocalDate().toEpochDay());
                return new RegisterInfoResponse(info.getId(), info.getNickname(), days, info.getCity());
            } catch (Exception e) {
                // Feign 调用失败降级返回 null，不阻断欢迎语生成
                return null;
            }
        };
    }

    @Bean
    @Description("获取当前登录用户所在城市的实时天气，包括温度、天气现象、风向、湿度。无需传参。")
    public Function<FunctionEmptyRequest, WeatherResponse> getCurrentWeather(
            UserClient userClient, QWeatherService qWeatherService) {
        return req -> {
            Long userId = UserContext.getUserId();
            if (userId == null) {
                return null;
            }
            try {
                String city = null;
                R<RegisterInfoDTO> resp = userClient.getRegisterInfo(userId);
                if (resp != null && resp.getData() != null) {
                    city = resp.getData().getCity();
                }
                WeatherInfo weather = qWeatherService.getCurrentWeather(city);
                if (weather == null) {
                    return null;
                }
                return new WeatherResponse(
                        weather.getCity(), weather.getTemp(), weather.getTempMax(), weather.getTempMin(),
                        weather.getText(), weather.getWindDir(), weather.getHumidity());
            } catch (Exception e) {
                return null;
            }
        };
    }
}
