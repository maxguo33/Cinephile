package com.mg.cinephile.source;

import com.mg.cinephile.domain.Screening;

import java.util.List;

public interface ScreeningSource {

    String getName();

    List<Screening> fetchScreenings();
}