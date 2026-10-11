package com.example.ksl.wsd2025.api.item;

import com.example.ksl.wsd2025.api.dto.ItemDto;
import com.example.ksl.wsd2025.api.request.ItemCreateRequest;
import com.example.ksl.wsd2025.api.request.ItemUpdateRequest;
import com.example.ksl.wsd2025.api.request.PriceUpdateRequest;
import com.example.ksl.wsd2025.common.ApiException;
import com.example.ksl.wsd2025.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/items")
public class ItemApiController {

    private final Map<Long, ItemDto> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    // [POST 1] 단건 생성 → 201 / 400 / 409
    @PostMapping
    public ResponseEntity<ApiResponse<ItemDto>> createItem(@Valid @RequestBody ItemCreateRequest request) {
        checkDuplicateName(request.getName(), null);
        ItemDto item = save(request.getName(), request.getPrice());
        // 강의: 201 Created + Location 헤더
        return ResponseEntity.created(URI.create("/api/items/" + item.getId()))
                .body(ApiResponse.success(item, "아이템이 생성되었습니다."));
    }

    // [POST 2] 일괄 생성 → 201 / 400 / 409
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ItemDto>>> createItems(@RequestBody List<ItemCreateRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "생성할 아이템 목록이 비어 있습니다.");
        }
        Set<String> names = new HashSet<>();
        for (ItemCreateRequest r : requests) {
            if (r.getName() == null || r.getName().isBlank() || r.getPrice() == null || r.getPrice() < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "각 아이템은 name(필수)과 price(0 이상)를 가져야 합니다.");
            }
            if (!names.add(r.getName())) {
                throw new ApiException(HttpStatus.CONFLICT,
                        "요청 목록 안에 중복된 이름이 있습니다: " + r.getName());
            }
            checkDuplicateName(r.getName(), null);
        }
        List<ItemDto> created = requests.stream().map(r -> save(r.getName(), r.getPrice())).toList();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, created.size() + "개의 아이템이 생성되었습니다."));
    }

    // [GET 1] 목록·검색·페이징 → 200 / 400 / 500(simulateError=true)
    @GetMapping
    public ResponseEntity<ApiResponse<List<ItemDto>>> getItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean simulateError) {
        if (simulateError) {
            throw new IllegalStateException("의도적으로 발생시킨 서버 오류입니다.");
        }
        if (page < 0 || size <= 0 || size > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page는 0 이상, size는 1~100 사이여야 합니다.");
        }
        List<ItemDto> result = store.values().stream()
                .filter(i -> keyword == null || i.getName().contains(keyword))
                .sorted(Comparator.comparing(ItemDto::getId))
                .skip((long) page * size)
                .limit(size)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // [GET 2] 단건 조회 → 200 / 404
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(findOrThrow(id)));
    }

    // [PUT 1] 전체 수정 → 200 / 400 / 404 / 409
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> updateItem(
            @PathVariable Long id, @Valid @RequestBody ItemUpdateRequest request) {
        ItemDto item = findOrThrow(id);
        checkDuplicateName(request.getName(), id);
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        return ResponseEntity.ok(ApiResponse.success(item, "아이템이 수정되었습니다."));
    }

    // [PUT 2] 가격만 수정 → 200 / 400 / 404
    @PutMapping("/{id}/price")
    public ResponseEntity<ApiResponse<ItemDto>> updatePrice(
            @PathVariable Long id, @Valid @RequestBody PriceUpdateRequest request) {
        ItemDto item = findOrThrow(id);
        item.setPrice(request.getPrice());
        return ResponseEntity.ok(ApiResponse.success(item, "가격이 수정되었습니다."));
    }

    // [DELETE 1] 단건 삭제 → 200 / 404
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<ItemDto>> deleteItem(@PathVariable Long id) {
        ItemDto removed = store.remove(id);
        if (removed == null) throw notFound(id);
        return ResponseEntity.ok(ApiResponse.success(removed, "아이템이 삭제되었습니다."));
    }

    // [DELETE 2] 전체 삭제 (confirm=true 필수) → 200 / 400
    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, Integer>>> deleteAll(
            @RequestParam(defaultValue = "false") boolean confirm) {
        if (!confirm) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "전체 삭제하려면 confirm=true 파라미터가 필요합니다.");
        }
        int count = store.size();
        store.clear();
        return ResponseEntity.ok(ApiResponse.success(Map.of("deletedCount", count), "전체 아이템이 삭제되었습니다."));
    }

    // ---------- helpers ----------
    private ItemDto save(String name, Integer price) {
        ItemDto item = new ItemDto();
        item.setId(sequence.getAndIncrement());
        item.setName(name);
        item.setPrice(price);
        store.put(item.getId(), item);
        return item;
    }

    private ItemDto findOrThrow(Long id) {
        ItemDto item = store.get(id);
        if (item == null) throw notFound(id);
        return item;
    }

    private ApiException notFound(Long id) {
        return new ApiException(HttpStatus.NOT_FOUND, "id=" + id + " 아이템을 찾을 수 없습니다.");
    }

    private void checkDuplicateName(String name, Long selfId) {
        boolean dup = store.values().stream()
                .anyMatch(i -> i.getName().equals(name) && !i.getId().equals(selfId));
        if (dup) throw new ApiException(HttpStatus.CONFLICT, "이미 존재하는 이름입니다: " + name);
    }
}
