package dailyquest.user.controller

import dailyquest.context.IntegrationTestContext
import dailyquest.context.MockElasticsearchTestContextConfig
import dailyquest.context.MockRedisTestContextConfig
import dailyquest.user.dto.UserUpdateRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON_UTF8
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalTime

@Import(MockRedisTestContextConfig::class, MockElasticsearchTestContextConfig::class)
@DisplayName("유저 API 컨트롤러 통합 테스트")
class UserControllerTest : IntegrationTestContext() {

    private val uriPrefix = "/api/v1/users"

    @Nested
    @DisplayName("유저 설정 수정 시")
    inner class UserSettingsUpdateTest {
        @DisplayName("설정값 변경 후 24시간이 지나지 않았다면 오류가 발생한다")
        @Test
        fun `설정값 변경 후 24시간이 지나지 않았다면 오류가 발생한다`() {
            //given
            val url = "${SERVER_ADDR}$port${uriPrefix}"

            val firstDto = UserUpdateRequest(
                coreTime = LocalTime.of(user.getCoreHour(), 0).plusHours(1).hour
            )

            //when
            val firstRequest = mvc
                .perform(
                    patch(url)
                        .contentType(APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(userToken)
                        .content(om.writeValueAsString(firstDto))
                )

            val secondDto = UserUpdateRequest(
                coreTime = LocalTime.of(user.getCoreHour(), 0).minusHours(1).hour
            )

            val secondRequest = mvc
                .perform(
                    patch(url)
                        .contentType(APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(userToken)
                        .content(om.writeValueAsString(secondDto))
                )

            //then
            firstRequest.andExpect(status().isOk)
            secondRequest.andExpect(status().isBadRequest)
            val afterUser = userRepository.findById(user.id).get()
            assertThat(afterUser.getCoreHour()).isEqualTo(firstDto.coreTime)
            assertThat(afterUser.getCoreHour()).isNotEqualTo(secondDto.coreTime)
        }

    }
}