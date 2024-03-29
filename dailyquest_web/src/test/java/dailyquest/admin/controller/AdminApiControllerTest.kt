package dailyquest.admin.controller

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import dailyquest.admin.controller.AdminApiControllerTest.AdminApiControllerIntegrationConfig
import dailyquest.admin.dto.SystemSettingsRequest
import dailyquest.admin.dto.SystemSettingsResponse
import dailyquest.admin.service.AdminService
import dailyquest.common.CustomRedisContainer
import dailyquest.common.ResponseData
import dailyquest.context.IntegrationTestContextBaseConfig
import dailyquest.context.RedisTestContextConfig
import dailyquest.jwt.JwtTokenProvider
import dailyquest.user.entity.ProviderType
import dailyquest.user.entity.RoleType
import dailyquest.user.entity.UserInfo
import dailyquest.user.repository.UserRepository
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@Suppress("DEPRECATION")
@DisplayName("관리자 API 컨트롤러 통합 테스트")
@Transactional
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [AdminApiControllerIntegrationConfig::class]
)
class AdminApiControllerTest @Autowired constructor(
    val adminService: AdminService,
    val userRepository: UserRepository,
    val context: WebApplicationContext,
) {


    @ComponentScan(basePackages = ["dailyquest.admin"])
    @Import(
        IntegrationTestContextBaseConfig::class,
        RedisTestContextConfig::class,
    )
    class AdminApiControllerIntegrationConfig

    companion object {
        const val SERVER_ADDR = "http://localhost:"
        const val URI_PREFIX = "/admin/api/v1"

        @JvmStatic
        @Container
        val redis = CustomRedisContainer()

        @BeforeAll
        @JvmStatic
        fun initRedis() {
            redis.initRedis()
        }
    }

    @LocalServerPort
    var port = 0

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    lateinit var mvc: MockMvc
    lateinit var userToken: Cookie
    lateinit var adminToken: Cookie
    val om: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule())

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders
            .webAppContextSetup(context)
            .addFilter<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .apply<DefaultMockMvcBuilder>(SecurityMockMvcConfigurers.springSecurity())
            .build()

        val user = UserInfo("", "user1", ProviderType.GOOGLE)
        val admin = UserInfo("", "user2", ProviderType.GOOGLE)
        admin.role = RoleType.ADMIN

        val savedUser = userRepository.save(user)
        val savedAdmin = userRepository.save(admin)

        val accessToken1 = jwtTokenProvider.createAccessToken(savedUser.id)
        userToken = jwtTokenProvider.createAccessTokenCookie(accessToken1)

        val accessToken2 = jwtTokenProvider.createAccessToken(savedAdmin.id)
        adminToken = jwtTokenProvider.createAccessTokenCookie(accessToken2)
    }


    @DisplayName("시스템 설정 조회 시")
    @Nested
    inner class GetSystemSettingsTest {

        private val uriSuffix = "/reward"

        @DisplayName("관리자 권한이 아니면 FORBIDDEN이 반환된다")
        @Test
        fun `관리자 권한이 아니면 FORBIDDEN이 반환된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"

            //when
            val request = mvc
                .perform(
                    get(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(userToken)
                )

            //then
            request
                .andExpect(status().isForbidden)
        }

        @DisplayName("관리자 권한이라면 시스템 세팅이 반환된다")
        @Test
        fun `관리자 권한이라면 시스템 세팅이 반환된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"

            //when
            val request = mvc
                .perform(
                    get(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(adminToken)
                )

            //then
            val body = request
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()
                .response
                .contentAsString

            val result = om.readValue(body, object: TypeReference<ResponseData<SystemSettingsResponse>>(){})
        }
    }

    @DisplayName("시스템 설정 변경 시")
    @Nested
    inner class UpdateSystemSettingsTest {
        private val uriSuffix = "/reward"

        @DisplayName("관리자 권한이 아니면 FORBIDDEN이 반환된다")
        @Test
        fun `관리자 권한이 아니면 FORBIDDEN이 반환된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"

            val requestDto = SystemSettingsRequest(5, 5, 10)
            val requestBody = om.writeValueAsString(requestDto)

            //when
            val request = mvc
                .perform(
                    put(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(userToken)
                        .content(requestBody)
                )

            //then
            request
                .andExpect(status().isForbidden)
        }

        @DisplayName("관리자 권한이라면 시스템 세팅이 변경된다")
        @Test
        fun `관리자 권한이라면 시스템 세팅이 변경된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"

            val requestDto = SystemSettingsRequest(5, 5, 10)
            val requestBody = om.writeValueAsString(requestDto)

            //when
            val request = mvc
                .perform(
                    put(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(adminToken)
                        .content(requestBody)
                )

            //then
            request
                .andExpect(status().isOk)

            val systemSettings = adminService.getSystemSettings()

            assertThat(systemSettings.maxRewardCount).isEqualTo(requestDto.maxRewardCount)
            assertThat(systemSettings.questClearExp).isEqualTo(requestDto.questClearExp)
            assertThat(systemSettings.questClearGold).isEqualTo(requestDto.questClearGold)

        }
    }

    @DisplayName("경험치 테이블 조회 시")
    @Nested
    inner class GetExpTableTest {

        private val uriSuffix = "/exp-table"
        @DisplayName("관리자 권한이 아니면 FORBIDDEN이 반환된다")
        @Test
        fun `관리자 권한이 아니면 FORBIDDEN이 반환된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"

            //when
            val request = mvc
                .perform(
                    get(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(userToken)
                )

            //then
            request
                .andExpect(status().isForbidden)
        }

        @DisplayName("관리자 권한이라면 경험치 테이블이 반환된다")
        @Test
        fun `관리자 권한이라면 경험치 테이블이 반환된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"

            //when
            val request = mvc
                .perform(
                    get(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(adminToken)
                )

            //then
            val body = request
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andReturn()
                .response
                .contentAsString

            val result = om.readValue(body, object: TypeReference<ResponseData<Map<String, Long>>>(){})
        }
    }


    @DisplayName("경험치 테이블 변경 시")
    @Nested
    inner class UpdateExpTableTest {

        private val uriSuffix = "/exp-table"
        @DisplayName("관리자 권한이 아니면 FORBIDDEN이 반환된다")
        @Test
        fun `관리자 권한이 아니면 FORBIDDEN이 반환된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"
            val requestMap = mapOf(1 to 5, 2 to 5, 3 to 10)
            val requestBody = om.writeValueAsString(requestMap)

            //when
            val request = mvc
                .perform(
                    put(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(userToken)
                        .content(requestBody)
                )

            //then
            request
                .andExpect(status().isForbidden)
        }

        @DisplayName("관리자 권한이라면 경험치 테이블이 변경된다")
        @Test
        fun `관리자 권한이라면 경험치 테이블이 변경된다`() {
            //given
            val url = "$SERVER_ADDR$port$URI_PREFIX$uriSuffix"
            val requestMap = mapOf(1 to 5L, 2 to 5L, 3 to 10L)
            val requestBody = om.writeValueAsString(requestMap)

            //when
            val request = mvc
                .perform(
                    put(url)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .with(csrf())
                        .cookie(adminToken)
                        .content(requestBody)
                )

            //then
            request
                .andExpect(status().isOk)

            val expTable = adminService.getExpTable()

            assertThat(expTable[1]).isEqualTo(requestMap[1])
            assertThat(expTable[2]).isEqualTo(requestMap[2])
            assertThat(expTable[3]).isEqualTo(requestMap[3])
        }
    }


}