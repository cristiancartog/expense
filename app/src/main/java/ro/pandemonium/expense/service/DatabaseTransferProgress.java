package ro.pandemonium.expense.service;

public record DatabaseTransferProgress(int completed, int total, boolean restore) {
}
