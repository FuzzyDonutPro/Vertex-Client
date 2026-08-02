<script>
    import { onMount, onDestroy } from 'svelte';

    export let visible = true;

    let authenticated = false;
    let isPlaying = false;
    let trackName = 'No Track Playing';
    let artistName = 'Spotify';
    let albumName = '';
    let albumArtUrl = '';
    let progressMs = 0;
    let durationMs = 0;

    let intervalId;

    function sendIpc(request, onSuccess) {
        if (window.cefQuery) {
            window.cefQuery({
                request: request,
                onSuccess: (response) => {
                    if (onSuccess) onSuccess(response);
                },
                onFailure: (errCode, errMsg) => {
                    console.error('[Spotify IPC Error]', errCode, errMsg);
                }
            });
        }
    }

    function fetchStatus() {
        sendIpc('spotify_get_status', (resp) => {
            try {
                const data = JSON.parse(resp);
                if (data.status === 'ok') {
                    authenticated = data.authenticated;
                    isPlaying = data.isPlaying;
                    trackName = data.trackName || 'No Track Playing';
                    artistName = data.artistName || 'Spotify';
                    albumName = data.albumName || '';
                    albumArtUrl = data.albumArtUrl || '';
                    progressMs = data.progressMs || 0;
                    durationMs = data.durationMs || 0;
                }
            } catch (e) {
                console.error('Failed to parse Spotify status response:', e);
            }
        });
    }

    function triggerAuth() {
        sendIpc('spotify_auth');
    }

    function togglePlay() {
        sendIpc('spotify_toggle', () => fetchStatus());
    }

    function nextTrack() {
        sendIpc('spotify_next', () => fetchStatus());
    }

    function prevTrack() {
        sendIpc('spotify_prev', () => fetchStatus());
    }

    $: progressPercent = durationMs > 0 ? Math.min(100, Math.max(0, (progressMs / durationMs) * 100)) : 0;

    onMount(() => {
        fetchStatus();
        intervalId = setInterval(fetchStatus, 2000);
    });

    onDestroy(() => {
        if (intervalId) clearInterval(intervalId);
    });
</script>

{#if visible}
<div class="spotify-card">
    <div class="spotify-header">
        <div class="spotify-brand">
            <svg class="spotify-logo" viewBox="0 0 24 24" width="18" height="18" fill="currentColor">
                <path d="M12 0C5.376 0 0 5.376 0 12s5.376 12 12 12 12-5.376 12-12S18.624 0 12 0zm5.521 17.341c-.219.359-.691.475-1.049.257-2.873-1.756-6.488-2.153-10.749-1.181-.407.093-.815-.162-.908-.569-.093-.407.162-.815.569-.908 4.656-1.066 8.665-.609 11.874 1.353.358.218.474.69.256 1.048zm1.474-3.273c-.276.448-.863.592-1.31.317-3.287-2.02-8.301-2.607-12.19-1.427-.502.152-1.036-.134-1.188-.636-.153-.502.134-1.036.636-1.188 4.444-1.35 9.983-.7 13.735 1.607.447.276.592.863.317 1.327zm.129-3.41c-.332.527-1.03.693-1.557.362-3.83-2.316-9.84-2.518-13.376-1.444-.602.183-1.241-.168-1.424-.77-.183-.602.168-1.241.77-1.424 4.195-1.274 10.829-1.026 15.225 1.719.527.331.693 1.03.362 1.557z"/>
            </svg>
            <span>Spotify Player</span>
        </div>
        {#if !authenticated}
            <button class="btn-auth" on:click={triggerAuth}>Link Account</button>
        {:else}
            <span class="status-pill active">Connected</span>
        {/if}
    </div>

    {#if authenticated}
    <div class="spotify-body">
        <div class="art-container">
            {#if albumArtUrl}
                <img src={albumArtUrl} alt="Album Art" class="album-art" />
            {:else}
                <div class="art-placeholder">🎵</div>
            {/if}
        </div>

        <div class="track-details">
            <div class="track-title" title={trackName}>{trackName}</div>
            <div class="artist-name" title={artistName}>{artistName}</div>
            {#if albumName}
                <div class="album-name" title={albumName}>{albumName}</div>
            {/if}
        </div>
    </div>

    <div class="progress-bar-container">
        <div class="progress-bar-fill" style="width: {progressPercent}%"></div>
    </div>

    <div class="spotify-controls">
        <button class="ctrl-btn" on:click={prevTrack} title="Previous">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
            </svg>
        </button>
        <button class="ctrl-btn main" on:click={togglePlay} title={isPlaying ? "Pause" : "Play"}>
            {#if isPlaying}
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
                </svg>
            {:else}
                <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                    <path d="M8 5v14l11-7z"/>
                </svg>
            {/if}
        </button>
        <button class="ctrl-btn" on:click={nextTrack} title="Next">
            <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                <path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/>
            </svg>
        </button>
    </div>
    {:else}
    <div class="unauth-banner">
        <p>Authorize Spotify to control music and view live playback directly inside Vertex Client.</p>
    </div>
    {/if}
</div>
{/if}

<style>
    .spotify-card {
        background: rgba(18, 18, 18, 0.75);
        backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 12px;
        padding: 14px 16px;
        color: #ffffff;
        font-family: 'Inter', system-ui, -apple-system, sans-serif;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4);
        max-width: 320px;
        margin: 10px 0;
    }

    .spotify-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
    }

    .spotify-brand {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: 600;
        font-size: 0.9rem;
        color: #1DB954;
    }

    .spotify-logo {
        fill: #1DB954;
    }

    .status-pill {
        font-size: 0.7rem;
        padding: 2px 8px;
        border-radius: 12px;
        background: rgba(29, 185, 84, 0.15);
        color: #1DB954;
        border: 1px solid rgba(29, 185, 84, 0.3);
    }

    .btn-auth {
        background: #1DB954;
        color: #000;
        border: none;
        border-radius: 16px;
        padding: 4px 12px;
        font-size: 0.75rem;
        font-weight: 700;
        cursor: pointer;
        transition: transform 0.15s ease, background 0.15s ease;
    }

    .btn-auth:hover {
        background: #1ed760;
        transform: scale(1.04);
    }

    .spotify-body {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 10px;
    }

    .album-art {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        object-fit: cover;
        box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
    }

    .art-placeholder {
        width: 48px;
        height: 48px;
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.05);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 1.2rem;
    }

    .track-details {
        overflow: hidden;
    }

    .track-title {
        font-weight: 600;
        font-size: 0.85rem;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        color: #fff;
    }

    .artist-name {
        font-size: 0.75rem;
        color: #b3b3b3;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .album-name {
        font-size: 0.68rem;
        color: #727272;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .progress-bar-container {
        width: 100%;
        height: 4px;
        background: rgba(255, 255, 255, 0.1);
        border-radius: 2px;
        overflow: hidden;
        margin-bottom: 10px;
    }

    .progress-bar-fill {
        height: 100%;
        background: #1DB954;
        transition: width 0.3s linear;
    }

    .spotify-controls {
        display: flex;
        justify-content: center;
        align-items: center;
        gap: 16px;
    }

    .ctrl-btn {
        background: transparent;
        border: none;
        color: #b3b3b3;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: color 0.15s ease, transform 0.15s ease;
    }

    .ctrl-btn:hover {
        color: #fff;
        transform: scale(1.1);
    }

    .ctrl-btn.main {
        background: #1DB954;
        color: #000;
        width: 32px;
        height: 32px;
        border-radius: 50%;
    }

    .ctrl-btn.main:hover {
        background: #1ed760;
        transform: scale(1.08);
    }

    .unauth-banner {
        font-size: 0.75rem;
        color: #aaaaaa;
        line-height: 1.4;
        text-align: center;
        padding: 8px 0;
    }
</style>
