//
//  PlaylistLocalStorage.swift
//  PMVPTests
//
//  Created by Aubrey Goodman on 4/8/19.
//  Copyright © 2019 Aubrey Goodman. All rights reserved.
//

@testable import PMVP

class PlaylistLocalStorage: LocalStorage<String, Playlist, PlaylistProxy, PlaylistError> {

	var playlists: [String: PlaylistProxy] = [:]

	private let testQueue = DispatchQueue(label: "test.local")

	override func allObjects(queue: DispatchQueue, callback: @escaping (Result<[PlaylistProxy], PlaylistError>) -> Void) {
		testQueue.async { [weak self] in
			if let playlists = self?.playlists {
				let results = [PlaylistProxy](playlists.values)
				queue.async { callback(.success(results)) }
			}
		}
	}

	override func object(for key: String, queue: DispatchQueue, callback: @escaping (Result<PlaylistProxy?, PlaylistError>) -> Void) {
		testQueue.async { [weak self] in
			let result = self?.playlists[key]
			queue.async { callback(.success(result)) }
		}
	}

	override func objects(for keys: [String], queue: DispatchQueue, callback: @escaping (Result<[PlaylistProxy], PlaylistError>) -> Void) {
		testQueue.async { [weak self] in
			var results: [PlaylistProxy] = []
			for key in keys {
				if let v = self?.playlists[key] {
					results.append(v)
				}
			}
			queue.async { callback(.success(results)) }
		}
	}

	override func update(_ object: PlaylistProxy, queue: DispatchQueue, callback: @escaping (Result<PlaylistProxy, PlaylistError>) -> Void) {
		testQueue.async { [weak self] in
			self?.playlists[object.key] = object
			queue.async { callback(.success(object)) }
		}
	}

	override func update(_ objects: [PlaylistProxy], queue: DispatchQueue, callback: @escaping (Result<[PlaylistProxy], PlaylistError>) -> Void) {
		testQueue.async { [weak self] in
			for obj in objects {
				self?.playlists[obj.key] = obj
			}
			queue.async { callback(.success(objects)) }
		}
	}

	override func destroy(_ object: PlaylistProxy, queue: DispatchQueue, callback: @escaping (Result<PlaylistProxy, PlaylistError>) -> Void) {
		testQueue.async { [weak self] in
			self?.playlists.removeValue(forKey: object.key)
			queue.async { callback(.success(object)) }
		}
	}

	override func destroy(_ objects: [PlaylistProxy], queue: DispatchQueue, callback: @escaping (Result<[PlaylistProxy], PlaylistError>) -> Void) {
		testQueue.async { [weak self] in
			var results: [PlaylistProxy] = []
			for obj in objects {
				if let v = self?.playlists[obj.key] {
					results.append(v)
				}
			}
			queue.async { callback(.success(results)) }
		}
	}

}
