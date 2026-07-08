package com.mysocial.media

import org.springframework.data.jpa.repository.JpaRepository

interface MediaAssetRepository : JpaRepository<MediaAsset, Long>
