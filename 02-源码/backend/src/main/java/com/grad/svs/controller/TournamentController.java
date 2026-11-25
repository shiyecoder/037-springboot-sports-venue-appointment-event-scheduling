package com.grad.svs.controller;

import com.grad.svs.common.R;
import com.grad.svs.entity.MatchSchedule;
import com.grad.svs.entity.Tournament;
import com.grad.svs.service.TournamentService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 赛事台账、赛程编排与成绩录入 */
@RestController
@RequestMapping("/api/tournament")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping("/page")
    public R<Object> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String status, @RequestParam(required = false) String sportType,
                          @RequestParam(required = false) Long venueId) {
        return R.ok(tournamentService.page(page, size, status, sportType, venueId));
    }

    @GetMapping("/{id}")
    public R<Object> detail(@PathVariable Long id) {
        return R.ok(tournamentService.detail(id));
    }

    @PostMapping
    public R<Tournament> save(@RequestBody Tournament t) {
        t.setId(null);
        return R.ok(tournamentService.save(t));
    }

    @PutMapping("/{id}")
    public R<Tournament> update(@PathVariable Long id, @RequestBody Tournament t) {
        t.setId(id);
        return R.ok(tournamentService.save(t));
    }

    @GetMapping("/{id}/conflicts")
    public R<Object> conflicts(@PathVariable Long id) {
        return R.ok(tournamentService.checkConflict(id));
    }

    @GetMapping("/gantt")
    public R<Object> gantt(@RequestParam(required = false) String date) {
        return R.ok(tournamentService.gantt(date));
    }

    @PostMapping("/match/{matchId}/score")
    public R<MatchSchedule> score(@PathVariable Long matchId, @RequestBody Map<String, Integer> body) {
        return R.ok(tournamentService.score(matchId, body.get("home"), body.get("away")));
    }
}
