package com.anysale.lead.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sales_playbook_step")
public class SalesPlaybookStep {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "playbook_id", nullable = false)
    private SalesPlaybook playbook;
    @Column(nullable = false) private int position;
    @Column(name = "delay_minutes", nullable = false) private int delayMinutes;
    @Column(nullable = false, length = 240) private String title;
    @Column(name = "task_type", nullable = false, length = 40) private String taskType;
    @Column(nullable = false, length = 20) private String priority = "NORMAL";
    @Column(length = 1000) private String note;

    public UUID getId() { return id; }
    public SalesPlaybook getPlaybook() { return playbook; } public void setPlaybook(SalesPlaybook value) { playbook = value; }
    public int getPosition() { return position; } public void setPosition(int value) { position = value; }
    public int getDelayMinutes() { return delayMinutes; } public void setDelayMinutes(int value) { delayMinutes = value; }
    public String getTitle() { return title; } public void setTitle(String value) { title = value; }
    public String getTaskType() { return taskType; } public void setTaskType(String value) { taskType = value; }
    public String getPriority() { return priority; } public void setPriority(String value) { priority = value; }
    public String getNote() { return note; } public void setNote(String value) { note = value; }
}
