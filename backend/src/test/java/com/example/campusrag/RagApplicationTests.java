package com.example.campusrag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.campusrag.domain.UserAccount;
import com.example.campusrag.repository.UserAccountRepository;
import com.example.campusrag.service.AuthService;
import com.example.campusrag.service.RagService;

@SpringBootTest
@ActiveProfiles("test")
class RagApplicationTests {
    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private RagService ragService;

    @Test
    void seedsUsersAndAnswersFromKnowledgeBase() {
        UserAccount student = authService.login("student", "123456");
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(3);

        RagService.AskResult result = ragService.ask(student, "校园卡丢了怎么补办？", 5, 0.1);
        assertThat(result.answer()).contains("校园卡");
        assertThat(result.sources()).isNotEmpty();
    }
}
