package md.botservice.repository;

import md.botservice.models.Report;
import md.botservice.models.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByOrderByReportedAtDesc();
    List<Report> findByStatusOrderByReportedAtDesc(ReportStatus status);

}