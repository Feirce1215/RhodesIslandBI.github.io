package com.rhodes.BI.manager;

import com.rhodes.BI.common.ErrorCode;
import com.rhodes.BI.constant.CommonConstant;
import com.rhodes.BI.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class AiManager {
    @Resource
    private YuCongMingClient client;

    public String doChat(String message) {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(CommonConstant.BI_MODEL_ID);
        devChatRequest.setMessage(message);

        BaseResponse<DevChatResponse> response = client.doChat(devChatRequest);
        if (response == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return response.getData().getContent();
    }
}
