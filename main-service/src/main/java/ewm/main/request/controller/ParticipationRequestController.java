package ewm.main.request.controller;

import ewm.main.dto.ParticipationRequestDto;
import ewm.main.request.service.ParticipationRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users/{userId}/requests")
@RequiredArgsConstructor
public class ParticipationRequestController {

    private final ParticipationRequestService requestService;

    /**
     * GET /users/{userId}/requests
     * Получение информации о заявках текущего пользователя на участие в чужих событиях
     */
    @GetMapping
    public List<ParticipationRequestDto> getRequests(@PathVariable long userId) {
        log.info("GET /users/{}/requests", userId);
        return requestService.getRequests(userId);
    }

    /**
     * POST /users/{userId}/requests?eventId=
     * Добавление запроса от текущего пользователя на участие в событии
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipationRequestDto addRequest(
            @PathVariable long userId,
            @RequestParam long eventId) {
        log.info("POST /users/{}/requests?eventId={}", userId, eventId);
        return requestService.addRequest(userId, eventId);
    }

    /**
     * PATCH /users/{userId}/requests/{requestId}/cancel
     * Отмена своего запроса на участие в событии
     */
    @PatchMapping("/{requestId}/cancel")
    public ParticipationRequestDto cancelRequest(
            @PathVariable long userId,
            @PathVariable long requestId) {
        log.info("PATCH /users/{}/requests/{}/cancel", userId, requestId);
        return requestService.cancelRequest(userId, requestId);
    }
}